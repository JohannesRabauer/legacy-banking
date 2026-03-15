# Plan 03 — Backend: Model Classes + DatabaseManager

## Output files
```
banking-service/src/main/java/com/legacybank/model/AccountInfo.java
banking-service/src/main/java/com/legacybank/model/TransactionRecord.java
banking-service/src/main/java/com/legacybank/service/DatabaseManager.java
```

---

## 1. `AccountInfo.java`

Plain JavaBean — no Lombok, no records, no builder pattern.
`@XmlRootElement` makes it serialisable by JAX-WS JAXB binding automatically.

```java
package com.legacybank.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;

@XmlRootElement(name = "accountInfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountNumber;
    private String ownerName;
    private double balance;    // Use double — old code, not BigDecimal

    // No-arg constructor (required by JAXB)
    public AccountInfo() { }

    // Full constructor
    public AccountInfo(String accountNumber, String ownerName, double balance) {
        this.accountNumber = accountNumber;
        this.ownerName    = ownerName;
        this.balance      = balance;
    }

    // Getters and Setters (old-style, verbose)
    public String getAccountNumber() { return accountNumber; }
    public void   setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getOwnerName()     { return ownerName; }
    public void   setOwnerName(String ownerName)         { this.ownerName = ownerName; }
    public double getBalance()       { return balance; }
    public void   setBalance(double balance)             { this.balance = balance; }
}
```

---

## 2. `TransactionRecord.java`

```java
package com.legacybank.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;

@XmlRootElement(name = "transactionRecord")
@XmlAccessorType(XmlAccessType.FIELD)
public class TransactionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private long   id;
    private String fromAccount;   // may be null
    private String toAccount;     // may be null
    private double amount;
    private String description;
    private String transactionDate;  // formatted as "yyyy-MM-dd HH:mm:ss" string — JAXB dates were painful in old code

    public TransactionRecord() { }

    public TransactionRecord(long id, String fromAccount, String toAccount,
                             double amount, String description, String transactionDate) {
        this.id              = id;
        this.fromAccount     = fromAccount;
        this.toAccount       = toAccount;
        this.amount          = amount;
        this.description     = description;
        this.transactionDate = transactionDate;
    }

    // Getters and setters for all fields
    public long   getId()              { return id; }
    public void   setId(long id)       { this.id = id; }

    public String getFromAccount()     { return fromAccount; }
    public void   setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }

    public String getToAccount()       { return toAccount; }
    public void   setToAccount(String toAccount) { this.toAccount = toAccount; }

    public double getAmount()          { return amount; }
    public void   setAmount(double amount) { this.amount = amount; }

    public String getDescription()     { return description; }
    public void   setDescription(String description) { this.description = description; }

    public String getTransactionDate() { return transactionDate; }
    public void   setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
}
```

Design note: `transactionDate` is stored as a `String` in the model — this was common in old SOAP code to avoid JAXB `XMLGregorianCalendar` pain.

---

## 3. `DatabaseManager.java`

Spring-managed bean but uses raw JDBC only — no JPA, no Hibernate, no Spring JDBC template.
Each method opens its own connection via `DriverManager.getConnection()` and closes it in a `finally` block.
Every query uses `PreparedStatement` with positional `?` parameters — no string concatenation in queries.

```java
package com.legacybank.service;

import com.legacybank.model.AccountInfo;
import com.legacybank.model.TransactionRecord;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    // Injected by Spring XML config
    private String jdbcUrl;
    private String username;
    private String password;

    // Static driver registration (old idiom)
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

    // --- getAccountInfo ---
    // SQL: SELECT account_number, owner_name, balance FROM accounts WHERE account_number = ?
    public AccountInfo getAccountInfo(String accountNumber) {
        // ... open connection, prepare statement, execute, build AccountInfo, close in finally
        // Returns null if account not found
    }

    // --- getBalance ---
    // SQL: SELECT balance FROM accounts WHERE account_number = ?
    // Returns -1.0 if account not found
    public double getBalance(String accountNumber) { ... }

    // --- transfer ---
    // Uses a manual transaction (conn.setAutoCommit(false)):
    //   1. SELECT balance FROM accounts WHERE account_number = ? FOR UPDATE  (lock both rows)
    //   2. Check from_account has sufficient funds
    //   3. UPDATE accounts SET balance = balance - ? WHERE account_number = ?
    //   4. UPDATE accounts SET balance = balance + ? WHERE account_number = ?
    //   5. INSERT INTO transactions (from_account, to_account, amount, description)
    //      VALUES (?, ?, ?, ?)
    //   6. conn.commit()
    //   On any error: conn.rollback()
    // Returns true on success, false on failure (insufficient funds, account not found)
    public boolean transfer(String fromAccount, String toAccount, double amount, String description) { ... }

    // --- getTransactionHistory ---
    // SQL: SELECT id, from_account, to_account, amount, description, transaction_date
    //      FROM transactions
    //      WHERE from_account = ? OR to_account = ?
    //      ORDER BY transaction_date ASC
    // Returns List<TransactionRecord> (ArrayList, not LinkedList)
    public List<TransactionRecord> getTransactionHistory(String accountNumber) { ... }

    // Spring setters (no annotations)
    public void setJdbcUrl(String jdbcUrl)     { this.jdbcUrl = jdbcUrl; }
    public void setUsername(String username)   { this.username = username; }
    public void setPassword(String password)   { this.password = password; }
}
```

### Important implementation details

#### Date formatting
Use `SimpleDateFormat("yyyy-MM-dd HH:mm:ss")` to convert `Timestamp` from the `ResultSet` to a string for `TransactionRecord.transactionDate`.

#### Resource cleanup pattern (pre-try-with-resources style)
```java
Connection conn = null;
PreparedStatement ps = null;
ResultSet rs = null;
try {
    conn = getConnection();
    ps = conn.prepareStatement("SELECT ...");
    ps.setString(1, accountNumber);
    rs = ps.executeQuery();
    // ...
} catch (SQLException e) {
    // log to System.err (old code, no SLF4J)
    System.err.println("DatabaseManager error: " + e.getMessage());
    return null; // or appropriate fallback
} finally {
    try { if (rs   != null) rs.close();   } catch (SQLException ignored) {}
    try { if (ps   != null) ps.close();   } catch (SQLException ignored) {}
    try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
}
```

#### Transfer method extra detail
```java
Connection conn = null;
PreparedStatement lockPs = null;
// ...
try {
    conn = getConnection();
    conn.setAutoCommit(false);

    // Lock source account row
    lockPs = conn.prepareStatement(
        "SELECT balance FROM accounts WHERE account_number = ? FOR UPDATE");
    lockPs.setString(1, fromAccount);
    ResultSet lockRs = lockPs.executeQuery();
    if (!lockRs.next()) {
        conn.rollback();
        return false; // source account not found
    }
    double fromBalance = lockRs.getDouble("balance");
    lockRs.close(); lockPs.close();

    if (fromBalance < amount) {
        conn.rollback();
        return false; // insufficient funds
    }

    // Debit source
    PreparedStatement debitPs = conn.prepareStatement(
        "UPDATE accounts SET balance = balance - ? WHERE account_number = ?");
    debitPs.setDouble(1, amount);
    debitPs.setString(2, fromAccount);
    debitPs.executeUpdate();
    debitPs.close();

    // Credit target
    PreparedStatement creditPs = conn.prepareStatement(
        "UPDATE accounts SET balance = balance + ? WHERE account_number = ?");
    creditPs.setDouble(1, amount);
    creditPs.setString(2, toAccount);
    int rows = creditPs.executeUpdate();
    creditPs.close();

    if (rows == 0) {
        conn.rollback();
        return false; // target account not found
    }

    // Record transaction
    PreparedStatement insertPs = conn.prepareStatement(
        "INSERT INTO transactions (from_account, to_account, amount, description) " +
        "VALUES (?, ?, ?, ?)");
    insertPs.setString(1, fromAccount);
    insertPs.setString(2, toAccount);
    insertPs.setDouble(3, amount);
    insertPs.setString(4, description);
    insertPs.executeUpdate();
    insertPs.close();

    conn.commit();
    return true;

} catch (SQLException e) {
    System.err.println("Transfer error: " + e.getMessage());
    try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
    return false;
} finally {
    try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
}
```

#### Error logging
Use `System.err.println(...)` only — no logging framework (SLF4J, Log4j) in the service layer. This is authentic Java EE early-2000s style.
