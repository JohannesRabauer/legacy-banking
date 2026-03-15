package com.legacybank.client;

import com.legacybank.client.stub.BankingService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class DashboardFrame extends JFrame {

    public DashboardFrame(String accountNumber, String ownerName, BankingService soapPort) {
        super("Legacy Banking System - " + ownerName);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLayout(new BorderLayout());

        JPanel banner = new JPanel(new GridLayout(2, 1));
        banner.setBackground(new Color(0, 70, 127));

        JLabel welcomeLabel = new JLabel("  Welcome, " + ownerName, JLabel.LEFT);
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel accountLabel = new JLabel("  Account: " + accountNumber, JLabel.LEFT);
        accountLabel.setForeground(Color.LIGHT_GRAY);
        accountLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        banner.add(welcomeLabel);
        banner.add(accountLabel);

        add(banner, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Balance", new BalancePanel(accountNumber, soapPort));
        tabs.addTab("Transfer", new TransferPanel(accountNumber, soapPort));
        tabs.addTab("History", new HistoryPanel(accountNumber, soapPort));

        add(tabs, BorderLayout.CENTER);
        setLocationRelativeTo(null);
    }
}
