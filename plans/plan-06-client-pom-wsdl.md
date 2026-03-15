# Plan 06 — Client: Maven POM + WSDL + Stub Generation

## Output files
```
banking-client/pom.xml
banking-client/src/main/resources/wsdl/banking.wsdl
```

---

## 1. `banking-client/pom.xml`

### Key metadata
```xml
<groupId>com.legacybank</groupId>
<artifactId>banking-client</artifactId>
<version>1.0</version>
<packaging>jar</packaging>
<name>Legacy Banking Client</name>
```

### Java version
```xml
<maven.compiler.source>1.8</maven.compiler.source>
<maven.compiler.target>1.8</maven.compiler.target>
```

### Dependencies

| artifact | version | purpose |
|---|---|---|
| `javax.xml.ws:jaxws-api` | `2.2.11` | compile — JAX-WS API |
| `com.sun.xml.ws:jaxws-rt` | `2.2.10` | compile — JAX-WS RI runtime for client-side stub invocation |

No Spring, no database driver, no logging frameworks — pure Swing client.

### Plugin: `jaxws-maven-plugin` for offline stub generation

```xml
<plugin>
  <groupId>org.jvnet.jax-ws-commons</groupId>
  <artifactId>jaxws-maven-plugin</artifactId>
  <version>2.3</version>
  <executions>
    <execution>
      <id>generate-stubs</id>
      <phase>generate-sources</phase>
      <goals>
        <goal>wsimport</goal>
      </goals>
      <configuration>
        <wsdlUrls>
          <!-- Read from local resources — no network call needed -->
          <wsdlUrl>${basedir}/src/main/resources/wsdl/banking.wsdl</wsdlUrl>
        </wsdlUrls>
        <packageName>com.legacybank.client.stub</packageName>
        <sourceDestDir>${project.build.directory}/generated-sources/wsimport</sourceDestDir>
        <destDir>${project.build.directory}/generated-classes/wsimport</destDir>
        <keep>true</keep>
        <verbose>true</verbose>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### Plugin: `maven-jar-plugin` for executable JAR

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-jar-plugin</artifactId>
  <version>2.6</version>
  <configuration>
    <archive>
      <manifest>
        <mainClass>com.legacybank.client.BankingApplication</mainClass>
        <addClasspath>true</addClasspath>
        <classpathPrefix>lib/</classpathPrefix>
      </manifest>
    </archive>
  </configuration>
</plugin>
```

### Plugin: `maven-dependency-plugin` for fat/flat lib directory

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-dependency-plugin</artifactId>
  <version>2.10</version>
  <executions>
    <execution>
      <id>copy-deps</id>
      <phase>package</phase>
      <goals><goal>copy-dependencies</goal></goals>
      <configuration>
        <outputDirectory>${project.build.directory}/lib</outputDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

This produces a runnable layout at `target/`:
```
target/
├── banking-client-1.0.jar   ← main JAR with Class-Path manifest entry
└── lib/
    ├── jaxws-rt-2.2.10.jar
    └── ... (transitive deps)
```

Run with: `java -jar target/banking-client-1.0.jar`

---

## 2. `banking.wsdl` — local copy for offline stub generation

This is a **hand-written** WSDL that exactly matches the service contract defined in Plans 03 & 04.
The client uses this for `wsimport` stub generation without needing the server running.

### Complete WSDL content

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions
    xmlns="http://schemas.xmlsoap.org/wsdl/"
    xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
    xmlns:tns="http://legacybank.com/banking"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    targetNamespace="http://legacybank.com/banking"
    name="BankingService">

  <types>
    <xsd:schema targetNamespace="http://legacybank.com/banking">

      <!-- getAccountInfo request/response -->
      <xsd:element name="getAccountInfo">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="accountNumber" type="xsd:string"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
      <xsd:element name="getAccountInfoResponse">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="return" type="tns:accountInfo" minOccurs="0"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>

      <!-- AccountInfo complex type -->
      <xsd:complexType name="accountInfo">
        <xsd:sequence>
          <xsd:element name="accountNumber" type="xsd:string" minOccurs="0"/>
          <xsd:element name="ownerName"     type="xsd:string" minOccurs="0"/>
          <xsd:element name="balance"       type="xsd:double"/>
        </xsd:sequence>
      </xsd:complexType>

      <!-- getBalance request/response -->
      <xsd:element name="getBalance">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="accountNumber" type="xsd:string"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
      <xsd:element name="getBalanceResponse">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="return" type="xsd:double"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>

      <!-- transfer request/response -->
      <xsd:element name="transfer">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="fromAccount"  type="xsd:string"/>
            <xsd:element name="toAccount"    type="xsd:string"/>
            <xsd:element name="amount"       type="xsd:double"/>
            <xsd:element name="description"  type="xsd:string"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
      <xsd:element name="transferResponse">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="return" type="xsd:boolean"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>

      <!-- getTransactionHistory request/response -->
      <xsd:element name="getTransactionHistory">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="accountNumber" type="xsd:string"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
      <xsd:element name="getTransactionHistoryResponse">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="return" type="tns:transactionRecord"
                         minOccurs="0" maxOccurs="unbounded"/>
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>

      <!-- TransactionRecord complex type -->
      <xsd:complexType name="transactionRecord">
        <xsd:sequence>
          <xsd:element name="id"              type="xsd:long"/>
          <xsd:element name="fromAccount"     type="xsd:string" minOccurs="0"/>
          <xsd:element name="toAccount"       type="xsd:string" minOccurs="0"/>
          <xsd:element name="amount"          type="xsd:double"/>
          <xsd:element name="description"     type="xsd:string" minOccurs="0"/>
          <xsd:element name="transactionDate" type="xsd:string" minOccurs="0"/>
        </xsd:sequence>
      </xsd:complexType>

    </xsd:schema>
  </types>

  <!-- Messages -->
  <message name="getAccountInfo">
    <part name="parameters" element="tns:getAccountInfo"/>
  </message>
  <message name="getAccountInfoResponse">
    <part name="parameters" element="tns:getAccountInfoResponse"/>
  </message>

  <message name="getBalance">
    <part name="parameters" element="tns:getBalance"/>
  </message>
  <message name="getBalanceResponse">
    <part name="parameters" element="tns:getBalanceResponse"/>
  </message>

  <message name="transfer">
    <part name="parameters" element="tns:transfer"/>
  </message>
  <message name="transferResponse">
    <part name="parameters" element="tns:transferResponse"/>
  </message>

  <message name="getTransactionHistory">
    <part name="parameters" element="tns:getTransactionHistory"/>
  </message>
  <message name="getTransactionHistoryResponse">
    <part name="parameters" element="tns:getTransactionHistoryResponse"/>
  </message>

  <!-- Port Type -->
  <portType name="BankingService">
    <operation name="getAccountInfo">
      <input  message="tns:getAccountInfo"/>
      <output message="tns:getAccountInfoResponse"/>
    </operation>
    <operation name="getBalance">
      <input  message="tns:getBalance"/>
      <output message="tns:getBalanceResponse"/>
    </operation>
    <operation name="transfer">
      <input  message="tns:transfer"/>
      <output message="tns:transferResponse"/>
    </operation>
    <operation name="getTransactionHistory">
      <input  message="tns:getTransactionHistory"/>
      <output message="tns:getTransactionHistoryResponse"/>
    </operation>
  </portType>

  <!-- Binding -->
  <binding name="BankingServicePortBinding" type="tns:BankingService">
    <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
    <operation name="getAccountInfo">
      <soap:operation soapAction=""/>
      <input>  <soap:body use="literal"/> </input>
      <output> <soap:body use="literal"/> </output>
    </operation>
    <operation name="getBalance">
      <soap:operation soapAction=""/>
      <input>  <soap:body use="literal"/> </input>
      <output> <soap:body use="literal"/> </output>
    </operation>
    <operation name="transfer">
      <soap:operation soapAction=""/>
      <input>  <soap:body use="literal"/> </input>
      <output> <soap:body use="literal"/> </output>
    </operation>
    <operation name="getTransactionHistory">
      <soap:operation soapAction=""/>
      <input>  <soap:body use="literal"/> </input>
      <output> <soap:body use="literal"/> </output>
    </operation>
  </binding>

  <!-- Service -->
  <service name="BankingService">
    <port name="BankingServicePort" binding="tns:BankingServicePortBinding">
      <soap:address location="http://localhost:8080/banking-service/ws/banking"/>
    </port>
  </service>

</definitions>
```

---

## Generated stub classes (after `mvn generate-sources`)

`wsimport` will generate into `target/generated-sources/wsimport/com/legacybank/client/stub/`:

| Class | Purpose |
|---|---|
| `BankingService.java` | `@WebService` service locator |
| `BankingService_Service.java` | `javax.xml.ws.Service` subclass — entry point for client code |
| `GetAccountInfo.java` | Request wrapper |
| `GetAccountInfoResponse.java` | Response wrapper |
| `AccountInfo.java` | JAXB-generated model (note: different package from server model) |
| `TransactionRecord.java` | JAXB-generated model |
| `GetBalance.java` | Request/response wrappers |
| `GetBalanceResponse.java` | |
| `Transfer.java` | |
| `TransferResponse.java` | |
| `GetTransactionHistory.java` | |
| `GetTransactionHistoryResponse.java` | |
| `ObjectFactory.java` | JAXB factory |
| `package-info.java` | Package annotation |

The Swing code will use `BankingService_Service` and `BankingService` to invoke operations.

### Stub usage pattern in Swing code
```java
import com.legacybank.client.stub.BankingService;
import com.legacybank.client.stub.BankingService_Service;
import com.legacybank.client.stub.AccountInfo;
import com.legacybank.client.stub.TransactionRecord;

// In client code:
BankingService_Service svc = new BankingService_Service();
BankingService port = svc.getBankingServicePort();

AccountInfo info = port.getAccountInfo("DE89370400440532013000");
boolean ok = port.transfer("DE89...", "DE91...", 100.0, "Test payment");
```

---

## Notes for implementation

- The local WSDL `soap:address location` hardcodes `http://localhost:8080/banking-service/ws/banking`.
  If connecting to a different host, the client Swing code can override the endpoint address at runtime:
  ```java
  ((BindingProvider) port).getRequestContext().put(
      BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
      "http://customhost:8080/banking-service/ws/banking");
  ```
  This is how old JAX-WS clients switched environments.
- The stub package `com.legacybank.client.stub` keeps generated code separate from hand-written Swing code.
