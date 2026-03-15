package com.legacybank.service;

import com.legacybank.model.AccountInfo;
import com.legacybank.model.TransactionRecord;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService(name = "BankingService", targetNamespace = "http://legacybank.com/banking")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
public interface BankingService {

    @WebMethod(operationName = "getAccountInfo")
    @WebResult(name = "return")
    AccountInfo getAccountInfo(@WebParam(name = "accountNumber") String accountNumber);

    @WebMethod(operationName = "getBalance")
    @WebResult(name = "return")
    double getBalance(@WebParam(name = "accountNumber") String accountNumber);

    @WebMethod(operationName = "transfer")
    @WebResult(name = "return")
    boolean transfer(
            @WebParam(name = "fromAccount") String fromAccount,
            @WebParam(name = "toAccount") String toAccount,
            @WebParam(name = "amount") double amount,
            @WebParam(name = "description") String description);

    @WebMethod(operationName = "getTransactionHistory")
    @WebResult(name = "return")
    List<TransactionRecord> getTransactionHistory(@WebParam(name = "accountNumber") String accountNumber);
}
