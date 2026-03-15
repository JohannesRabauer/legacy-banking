package com.legacybank.client;

import com.legacybank.client.stub.BankingService;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class TransferPanel extends JPanel {

    private final String fromAccount;
    private final BankingService soapPort;

    private JTextField toAccountField;
    private JTextField amountField;
    private JTextField descriptionField;
    private JLabel statusLabel;

    public TransferPanel(String fromAccount, BankingService soapPort) {
        this.fromAccount = fromAccount;
        this.soapPort = soapPort;
        buildUi();
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("To Account Number:"), gbc);

        toAccountField = new JTextField(22);
        gbc.gridx = 1;
        add(toAccountField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Amount (EUR):"), gbc);

        amountField = new JTextField(10);
        gbc.gridx = 1;
        add(amountField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Description:"), gbc);

        descriptionField = new JTextField(30);
        gbc.gridx = 1;
        add(descriptionField, gbc);

        JButton sendButton = new JButton("Send Money");
        sendButton.setBackground(new Color(0, 100, 0));
        sendButton.setForeground(Color.WHITE);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(sendButton, gbc);

        statusLabel = new JLabel(" ");
        gbc.gridy = 4;
        add(statusLabel, gbc);

        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                submitTransfer();
            }
        });
    }

    private void submitTransfer() {
        String toAccount = toAccountField.getText() == null ? "" : toAccountField.getText().trim();
        String amountText = amountField.getText() == null ? "" : amountField.getText().trim();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();

        if (toAccount.length() == 0) {
            JOptionPane.showMessageDialog(this, "Target account is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Amount must be a number.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (amount <= 0.0d) {
            JOptionPane.showMessageDialog(this, "Amount must be positive.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (description.length() == 0) {
            description = "Transfer";
        }

        try {
            boolean ok = soapPort.transfer(fromAccount, toAccount, amount, description);
            if (ok) {
                statusLabel.setForeground(new Color(0, 128, 0));
                statusLabel.setText("Transfer successful");
                JOptionPane.showMessageDialog(this, "EUR " + String.format("%.2f", amount) + " sent successfully.");
                toAccountField.setText("");
                amountField.setText("");
                descriptionField.setText("");
            } else {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Transfer failed");
                JOptionPane.showMessageDialog(this,
                        "Transfer failed. Check account numbers and available balance.",
                        "Transfer Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Service error");
            JOptionPane.showMessageDialog(this,
                    "Service error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.err.println("Transfer error: " + e.getMessage());
        }
    }
}
