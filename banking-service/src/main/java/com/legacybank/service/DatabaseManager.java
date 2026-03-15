package com.legacybank.service;

import com.legacybank.model.AccountInfo;
import com.legacybank.model.TransactionRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private String jdbcUrl;
    private String username;
    private String password;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public AccountInfo getAccountInfo(String accountNumber) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(
                    "SELECT account_number, owner_name, balance FROM accounts WHERE account_number = ?");
            ps.setString(1, accountNumber);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new AccountInfo(
                        rs.getString("account_number"),
                        rs.getString("owner_name"),
                        rs.getDouble("balance"));
            }
            return null;
        } catch (SQLException e) {
            System.err.println("getAccountInfo database error: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    public double getBalance(String accountNumber) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number = ?");
            ps.setString(1, accountNumber);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("balance");
            }
            return -1.0;
        } catch (SQLException e) {
            System.err.println("getBalance database error: " + e.getMessage());
            return -1.0;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    public boolean transfer(String fromAccount, String toAccount, double amount, String description) {
        if (amount <= 0.0d) {
            return false;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number = ? FOR UPDATE");
            ps.setString(1, fromAccount);
            rs = ps.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return false;
            }
            double fromBalance = rs.getDouble("balance");
            closeQuietly(rs);
            closeQuietly(ps);

            if (fromBalance < amount) {
                conn.rollback();
                return false;
            }

            ps = conn.prepareStatement("SELECT account_number FROM accounts WHERE account_number = ? FOR UPDATE");
            ps.setString(1, toAccount);
            rs = ps.executeQuery();
            if (!rs.next()) {
                conn.rollback();
                return false;
            }
            closeQuietly(rs);
            closeQuietly(ps);

            ps = conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE account_number = ?");
            ps.setDouble(1, amount);
            ps.setString(2, fromAccount);
            if (ps.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }
            closeQuietly(ps);

            ps = conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE account_number = ?");
            ps.setDouble(1, amount);
            ps.setString(2, toAccount);
            if (ps.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }
            closeQuietly(ps);

            ps = conn.prepareStatement(
                    "INSERT INTO transactions (from_account, to_account, amount, description) VALUES (?, ?, ?, ?)");
            ps.setString(1, fromAccount);
            ps.setString(2, toAccount);
            ps.setDouble(3, amount);
            ps.setString(4, description);
            if (ps.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("transfer database error: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            return false;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
            closeQuietly(conn);
        }
    }

    public List<TransactionRecord> getTransactionHistory(String accountNumber) {
        List<TransactionRecord> records = new ArrayList<TransactionRecord>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            ps = conn.prepareStatement(
                    "SELECT id, from_account, to_account, amount, description, transaction_date "
                            + "FROM transactions WHERE from_account = ? OR to_account = ? "
                            + "ORDER BY transaction_date ASC");
            ps.setString(1, accountNumber);
            ps.setString(2, accountNumber);
            rs = ps.executeQuery();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("transaction_date");
                String dateText = ts != null ? sdf.format(ts) : null;

                TransactionRecord record = new TransactionRecord(
                        rs.getLong("id"),
                        rs.getString("from_account"),
                        rs.getString("to_account"),
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        dateText);
                records.add(record);
            }
        } catch (SQLException e) {
            System.err.println("getTransactionHistory database error: " + e.getMessage());
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }

        return records;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeQuietly(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
