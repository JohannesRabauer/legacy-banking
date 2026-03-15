# Plan 04 — Backend: JAX-WS Service SEI + Implementation

## Output files
```
banking-service/src/main/java/com/legacybank/service/BankingService.java
banking-service/src/main/java/com/legacybank/service/BankingServiceImpl.java
```

---

## 1. `BankingService.java` — Service Endpoint Interface (SEI)

The SEI is a plain Java interface annotated with `@WebService`.
JAX-WS RI uses it to generate the WSDL contract at deploy time.

```java
package com.legacybank.service;

import com.legacybank.model.AccountInfo;
import com.legacybank.model.TransactionRecord;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.util.List;

@WebService(
    name            = "BankingService",
    targetNamespace = "http://legacybank.com/banking"
)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT,
             use   = SOAPBinding.Use.LITERAL)
public interface BankingService {

    @WebMethod(operationName = "getAccountInfo")
    @WebResult(name = "return")
    AccountInfo getAccountInfo(
        @WebParam(name = "accountNumber") String accountNumber
    );

    @WebMethod(operationName = "getBalance")
    @WebResult(name = "return")
    double getBalance(
        @WebParam(name = "accountNumber") String accountNumber
    );

    @WebMethod(operationName = "transfer")
    @WebResult(name = "return")
    boolean transfer(
        @WebParam(name = "fromAccount")   String fromAccount,
        @WebParam(name = "toAccount")     String toAccount,
        @WebParam(name = "amount")        double amount,
        @WebParam(name = "description")   String description
    );

    @WebMethod(operationName = "getTransactionHistory")
    @WebResult(name = "return")
    List<TransactionRecord> getTransactionHistory(
        @WebParam(name = "accountNumber") String accountNumber
    );
}
```

WSDL targetNamespace: `http://legacybank.com/banking`
SOAP style: Document/Literal (most common in 2000s enterprise)

---

## 2. `BankingServiceImpl.java` — Implementation

### Class annotations
```java
@WebService(
    serviceName      = "BankingService",
    portName         = "BankingServicePort",
    targetNamespace  = "http://legacybank.com/banking",
    endpointInterface = "com.legacybank.service.BankingService",
    wsdlLocation     = "WEB-INF/wsdl/banking.wsdl"  // optional — if not present JAX-WS generates one
)
```
Remove `wsdlLocation` since we're doing code-first. Let RI generate the WSDL on the fly.

### Spring integration (the old-school way)

JAX-WS RI instantiates the impl class directly — it is NOT a Spring bean created by `ApplicationContext`.  
To retrieve the Spring-managed `DatabaseManager`, use `SpringBeanAutowiringSupport` in the constructor:

```java
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
// ...

public class BankingServiceImpl implements BankingService {

    // Spring injects this via SpringBeanAutowiringSupport
    @Autowired
    private DatabaseManager databaseManager;

    public BankingServiceImpl() {
        // Tell Spring to inject @Autowired fields into this JAX-WS-managed instance
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }
    // ...
}
```

`@Autowired` is the ONLY Spring annotation used in the impl (minimal Spring contamination — everything else is JAX-WS).

Actually, since `applicationContext.xml` declares `bankingServiceImpl` as a Spring bean too, we have a dual wiring risk. Simplest fix: **do not declare `BankingServiceImpl` in the Spring XML** — use `SpringBeanAutowiringSupport` only. Remove `bankingServiceImpl` bean from `applicationContext.xml` (see Plan 02 correction below).

### Corrected `applicationContext.xml` content
```xml
<!-- Only DatabaseManager; BankingServiceImpl is NOT a Spring bean -->
<bean id="databaseManager" class="com.legacybank.service.DatabaseManager">
  <property name="jdbcUrl"  value="jdbc:postgresql://${DB_HOST:localhost}:5432/bankdb"/>
  <property name="username" value="${DB_USER:bankuser}"/>
  <property name="password" value="${DB_PASSWORD:bankpassword}"/>
</bean>
```

Add `<context:annotation-config/>` to allow `@Autowired` to be processed by `SpringBeanAutowiringSupport`:
```xml
xmlns:context="http://www.springframework.org/schema/context"
...
<context:annotation-config/>
```

### Full class skeleton

```java
package com.legacybank.service;

import com.legacybank.model.AccountInfo;
import com.legacybank.model.TransactionRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.jws.WebService;
import java.util.List;

@WebService(
    serviceName       = "BankingService",
    portName          = "BankingServicePort",
    targetNamespace   = "http://legacybank.com/banking",
    endpointInterface = "com.legacybank.service.BankingService"
)
public class BankingServiceImpl implements BankingService {

    @Autowired
    private DatabaseManager databaseManager;

    public BankingServiceImpl() {
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    @Override
    public AccountInfo getAccountInfo(String accountNumber) {
        System.out.println("getAccountInfo called for: " + accountNumber);
        AccountInfo info = databaseManager.getAccountInfo(accountNumber);
        if (info == null) {
            // Return a stub AccountInfo with "NOT FOUND" — old code returned empty objects
            return new AccountInfo(accountNumber, "NOT FOUND", 0.0);
        }
        return info;
    }

    @Override
    public double getBalance(String accountNumber) {
        System.out.println("getBalance called for: " + accountNumber);
        return databaseManager.getBalance(accountNumber);
    }

    @Override
    public boolean transfer(String fromAccount, String toAccount,
                            double amount, String description) {
        System.out.println("transfer: " + fromAccount + " -> " + toAccount
                           + "  EUR " + amount);
        if (amount <= 0) {
            System.err.println("Transfer rejected: non-positive amount " + amount);
            return false;
        }
        return databaseManager.transfer(fromAccount, toAccount, amount, description);
    }

    @Override
    public List<TransactionRecord> getTransactionHistory(String accountNumber) {
        System.out.println("getTransactionHistory called for: " + accountNumber);
        return databaseManager.getTransactionHistory(accountNumber);
    }
}
```

---

## Implementation notes

- `System.out.println` for all operational logs — authentic 2000s Java EE practice.
- `System.err.println` for all error messages — no SLF4J, no Log4j.
- No checked exception thrown across the SOAP boundary. If the DB is down, the operation returns a failure indicator (`null`, `false`, `-1.0`, empty list).
- The `@WebService` annotation on the impl must reference `endpointInterface` — this is crucial for JAX-WS RI to link the impl to the SEI's WSDL contract.
- Verify that `SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this)` finds the `WebApplicationContext` — it searches `ServletContext` attribute keys. As long as Spring's `ContextLoaderListener` runs before the JAX-WS `WSServletContextListener`, this works. The ordering in `web.xml` must be:
  1. `ContextLoaderListener` (Spring)
  2. `WSServletContextListener` (JAX-WS RI)

---

## WSDL fragment that will be generated (for reference when copying to client)

The auto-generated WSDL will be available at:
`http://localhost:8080/banking-service/ws/banking?wsdl`

It will look like:
```xml
<definitions targetNamespace="http://legacybank.com/banking"
             name="BankingService">
  <types>
    <xsd:schema>...</xsd:schema>
  </types>
  <message name="getAccountInfo">
    <part name="parameters" element="tns:getAccountInfo"/>
  </message>
  ...
  <portType name="BankingService">
    <operation name="getAccountInfo">...</operation>
    <operation name="getBalance">...</operation>
    <operation name="transfer">...</operation>
    <operation name="getTransactionHistory">...</operation>
  </portType>
  <binding name="BankingServicePortBinding" type="tns:BankingService">
    <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
    ...
  </binding>
  <service name="BankingService">
    <port name="BankingServicePort" binding="tns:BankingServicePortBinding">
      <soap:address location="http://localhost:8080/banking-service/ws/banking"/>
    </port>
  </service>
</definitions>
```

This WSDL content is what Plan 06 will copy into `banking-client/src/main/resources/wsdl/banking.wsdl`
with `soap:address location` pointing to `http://localhost:8080/banking-service/ws/banking`.
