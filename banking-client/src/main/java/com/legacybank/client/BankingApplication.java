package com.legacybank.client;

import com.legacybank.client.stub.BankingService;
import com.legacybank.client.stub.BankingService_Service;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import java.net.URL;

public class BankingApplication {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception ignored) {
        }

        try {
            URL localWsdl = BankingApplication.class.getClassLoader().getResource("wsdl/banking.wsdl");
            if (localWsdl == null) {
            throw new IllegalStateException("Local WSDL resource not found: wsdl/banking.wsdl");
            }

            QName serviceName = new QName("http://legacybank.com/banking", "BankingService");
            BankingService_Service service = new BankingService_Service(localWsdl, serviceName);
            final BankingService port = service.getBankingServicePort();

            ((BindingProvider) port).getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                    "http://localhost:8080/banking-service/banking");

            ((BindingProvider) port).getRequestContext().put(
                "com.sun.xml.ws.connect.timeout", Integer.valueOf(4000));
            ((BindingProvider) port).getRequestContext().put(
                "com.sun.xml.ws.request.timeout", Integer.valueOf(7000));

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JFrame frame = new JFrame("Legacy Banking System - Login");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setContentPane(new LoginPanel(frame, port));
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not initialize SOAP client:\n" + e.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
