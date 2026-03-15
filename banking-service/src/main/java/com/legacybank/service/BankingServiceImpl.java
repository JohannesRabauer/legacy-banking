package com.legacybank.service;

import com.legacybank.model.AccountInfo;
import com.legacybank.model.TransactionRecord;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

@WebService(
        serviceName = "BankingService",
        portName = "BankingServicePort",
        targetNamespace = "http://legacybank.com/banking",
        endpointInterface = "com.legacybank.service.BankingService")
public class BankingServiceImpl implements BankingService {

    @Autowired
    private DatabaseManager databaseManager;

    public BankingServiceImpl() {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    public AccountInfo getAccountInfo(String accountNumber) {
        System.out.println("getAccountInfo called for: " + accountNumber);
        try {
            AccountInfo info = databaseManager.getAccountInfo(accountNumber);
            if (info == null) {
                return new AccountInfo(accountNumber, "NOT FOUND", 0.0);
            }
            return info;
        } catch (Exception e) {
            System.err.println("getAccountInfo service error: " + e.getMessage());
            return new AccountInfo(accountNumber, "NOT FOUND", 0.0);
        }
    }

    public double getBalance(String accountNumber) {
        System.out.println("getBalance called for: " + accountNumber);
        try {
            return databaseManager.getBalance(accountNumber);
        } catch (Exception e) {
            System.err.println("getBalance service error: " + e.getMessage());
            return -1.0;
        }
    }

    public boolean transfer(String fromAccount, String toAccount, double amount, String description) {
        System.out.println("transfer called: " + fromAccount + " -> " + toAccount + " amount=" + amount);

        if (fromAccount == null || toAccount == null) {
            return false;
        }
        if (fromAccount.trim().length() == 0 || toAccount.trim().length() == 0) {
            return false;
        }
        if (amount <= 0.0d) {
            return false;
        }

        try {
            return databaseManager.transfer(fromAccount.trim(), toAccount.trim(), amount, description);
        } catch (Exception e) {
            System.err.println("transfer service error: " + e.getMessage());
            return false;
        }
    }

    public List<TransactionRecord> getTransactionHistory(String accountNumber) {
        System.out.println("getTransactionHistory called for: " + accountNumber);
        try {
            return databaseManager.getTransactionHistory(accountNumber);
        } catch (Exception e) {
            System.err.println("getTransactionHistory service error: " + e.getMessage());
            return new ArrayList<TransactionRecord>();
        }
    }
}
