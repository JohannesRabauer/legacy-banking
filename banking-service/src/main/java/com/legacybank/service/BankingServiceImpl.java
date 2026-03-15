package com.legacybank.service;

import com.legacybank.model.AccountInfo;
import com.legacybank.model.TransactionRecord;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@WebService(
        serviceName = "BankingService",
        portName = "BankingServicePort",
        targetNamespace = "http://legacybank.com/banking",
        endpointInterface = "com.legacybank.service.BankingService")
public class BankingServiceImpl implements BankingService {

    private DatabaseManager databaseManager;

    @Resource
    private WebServiceContext webServiceContext;

    public BankingServiceImpl() {
    }

    private DatabaseManager resolveDatabaseManager() {
        if (databaseManager != null) {
            return databaseManager;
        }

        if (webServiceContext == null || webServiceContext.getMessageContext() == null) {
            return null;
        }

        Object servletContextObj = webServiceContext.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
        if (!(servletContextObj instanceof ServletContext)) {
            return null;
        }

        ServletContext servletContext = (ServletContext) servletContextObj;
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (ctx == null) {
            return null;
        }

        Object bean = ctx.getBean("databaseManager");
        if (bean instanceof DatabaseManager) {
            databaseManager = (DatabaseManager) bean;
            return databaseManager;
        }

        return null;
    }

    public AccountInfo getAccountInfo(String accountNumber) {
        System.out.println("getAccountInfo called for: " + accountNumber);
        try {
            DatabaseManager db = resolveDatabaseManager();
            if (db == null) {
                System.err.println("DatabaseManager is not available");
                return new AccountInfo(accountNumber, "NOT FOUND", 0.0);
            }

            AccountInfo info = db.getAccountInfo(accountNumber);
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
            DatabaseManager db = resolveDatabaseManager();
            if (db == null) {
                System.err.println("DatabaseManager is not available");
                return -1.0;
            }
            return db.getBalance(accountNumber);
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
            DatabaseManager db = resolveDatabaseManager();
            if (db == null) {
                System.err.println("DatabaseManager is not available");
                return false;
            }
            return db.transfer(fromAccount.trim(), toAccount.trim(), amount, description);
        } catch (Exception e) {
            System.err.println("transfer service error: " + e.getMessage());
            return false;
        }
    }

    public List<TransactionRecord> getTransactionHistory(String accountNumber) {
        System.out.println("getTransactionHistory called for: " + accountNumber);
        try {
            DatabaseManager db = resolveDatabaseManager();
            if (db == null) {
                System.err.println("DatabaseManager is not available");
                return new ArrayList<TransactionRecord>();
            }
            return db.getTransactionHistory(accountNumber);
        } catch (Exception e) {
            System.err.println("getTransactionHistory service error: " + e.getMessage());
            return new ArrayList<TransactionRecord>();
        }
    }
}
