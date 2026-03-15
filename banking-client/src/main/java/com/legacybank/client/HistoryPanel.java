package com.legacybank.client;

import com.legacybank.client.stub.BankingService;
import com.legacybank.client.stub.TransactionRecord;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class HistoryPanel extends JPanel {

    private final String accountNumber;
    private final BankingService soapPort;

    private DefaultTableModel model;

    public HistoryPanel(String accountNumber, BankingService soapPort) {
        this.accountNumber = accountNumber;
        this.soapPort = soapPort;
        buildUi();
        loadHistory();
    }

    private void buildUi() {
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        top.add(refreshButton);
        add(top, BorderLayout.NORTH);

        Vector columns = new Vector();
        columns.add("ID");
        columns.add("From");
        columns.add("To");
        columns.add("Amount (EUR)");
        columns.add("Description");
        columns.add("Date");

        model = new DefaultTableModel(new Vector(), columns) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(20);
        table.setGridColor(Color.LIGHT_GRAY);
        table.getTableHeader().setBackground(new Color(0, 70, 127));
        table.getTableHeader().setForeground(Color.WHITE);

        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadHistory();
            }
        });
    }

    private void loadHistory() {
        try {
            List<TransactionRecord> records = soapPort.getTransactionHistory(accountNumber);
            model.setRowCount(0);

            for (int i = 0; i < records.size(); i++) {
                TransactionRecord r = records.get(i);
                Vector row = new Vector();
                row.add(Long.valueOf(r.getId()));
                row.add(r.getFromAccount() != null ? r.getFromAccount() : "(external)");
                row.add(r.getToAccount() != null ? r.getToAccount() : "(external)");
                row.add(String.format("%.2f", r.getAmount()));
                row.add(r.getDescription());
                row.add(r.getTransactionDate());
                model.addRow(row);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load history: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.err.println("History error: " + e.getMessage());
        }
    }
}
