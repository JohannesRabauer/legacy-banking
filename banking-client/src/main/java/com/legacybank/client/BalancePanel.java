package com.legacybank.client;

import com.legacybank.client.stub.BankingService;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class BalancePanel extends JPanel {

    private final String accountNumber;
    private final BankingService soapPort;

    private JLabel balanceValueLabel;

    public BalancePanel(String accountNumber, BankingService soapPort) {
        this.accountNumber = accountNumber;
        this.soapPort = soapPort;
        buildUi();
        refreshBalance();
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        JLabel title = new JLabel("Current Balance");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(title, gbc);

        balanceValueLabel = new JLabel("EUR 0.00");
        balanceValueLabel.setFont(new Font("Monospaced", Font.BOLD, 28));
        balanceValueLabel.setForeground(new Color(0, 128, 0));

        gbc.gridy = 1;
        add(balanceValueLabel, gbc);

        JButton refreshButton = new JButton("Refresh Balance");
        gbc.gridy = 2;
        add(refreshButton, gbc);

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshBalance();
            }
        });
    }

    private void refreshBalance() {
        try {
            double balance = soapPort.getBalance(accountNumber);
            balanceValueLabel.setText(String.format("EUR %.2f", balance));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Service error while loading balance: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.err.println("Balance error: " + e.getMessage());
        }
    }
}
