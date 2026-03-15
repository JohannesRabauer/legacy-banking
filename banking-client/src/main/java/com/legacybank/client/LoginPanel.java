package com.legacybank.client;

import com.legacybank.client.stub.AccountInfo;
import com.legacybank.client.stub.BankingService;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class LoginPanel extends JPanel {

    private final JFrame loginFrame;
    private final BankingService soapPort;

    private JTextField accountNumberField;

    public LoginPanel(JFrame loginFrame, BankingService soapPort) {
        this.loginFrame = loginFrame;
        this.soapPort = soapPort;
        buildUi();
    }

    private void buildUi() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("LEGACY BANKING SYSTEM");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(40, 40, 40));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        gbc.gridwidth = 1;

        JLabel accountLabel = new JLabel("Account Number:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(accountLabel, gbc);

        accountNumberField = new JTextField(22);
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(accountNumberField, gbc);

        JButton loginButton = new JButton("Login");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(loginButton, gbc);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }
        });
    }

    private void doLogin() {
        String accountNumber = accountNumberField.getText() != null
                ? accountNumberField.getText().trim() : "";
        if (accountNumber.length() == 0) {
            JOptionPane.showMessageDialog(
                    loginFrame,
                    "Please enter your account number.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            AccountInfo info = soapPort.getAccountInfo(accountNumber);
            if (info == null || "NOT FOUND".equals(info.getOwnerName())) {
                JOptionPane.showMessageDialog(
                        loginFrame,
                        "Account not found.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            loginFrame.dispose();
            DashboardFrame dashboard = new DashboardFrame(accountNumber, info.getOwnerName(), soapPort);
            dashboard.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    loginFrame,
                    "Service error: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.err.println("Login error: " + ex.getMessage());
        }
    }
}
