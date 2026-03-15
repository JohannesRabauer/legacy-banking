# Plan 02 — Backend: POM + XML Configuration Files

## Output files
```
banking-service/pom.xml
banking-service/src/main/webapp/WEB-INF/web.xml
banking-service/src/main/webapp/WEB-INF/sun-jaxws.xml
banking-service/src/main/webapp/WEB-INF/applicationContext.xml
```

---

## 1. `banking-service/pom.xml`

### Key metadata
```xml
<groupId>com.legacybank</groupId>
<artifactId>banking-service</artifactId>
<version>1.0</version>
<packaging>war</packaging>
<name>Legacy Banking Service</name>
```

### Java version
```xml
<maven.compiler.source>1.8</maven.compiler.source>
<maven.compiler.target>1.8</maven.compiler.target>
```

### Dependencies (exact versions — these are the authentic old artifacts)

| artifact | version | scope |
|---|---|---|
| `javax.xml.ws:jaxws-api` | `2.2.11` | compile |
| `com.sun.xml.ws:jaxws-rt` | `2.2.10` | compile (brings jaxws-ri runtime) |
| `org.springframework:spring-context` | `3.2.18.RELEASE` | compile |
| `org.springframework:spring-web` | `3.2.18.RELEASE` | compile |
| `org.postgresql:postgresql` | `9.3-1100-jdbc41` | compile |
| `javax.servlet:servlet-api` | `2.5` | provided |

### `maven-war-plugin` configuration
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-war-plugin</artifactId>
  <version>2.6</version>
  <configuration>
    <warName>banking-service</warName>
    <failOnMissingWebXml>true</failOnMissingWebXml>
  </configuration>
</plugin>
```

No other plugins needed (no wsgen — server stubs are generated at runtime by JAX-WS RI's `WSServletContextListener`).

---

## 2. `web.xml` — Servlet 2.5 style with DTD

Use the old Servlet 2.5 DOCTYPE (not namespace-based schema):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE web-app PUBLIC
   "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
   "http://java.sun.com/dtd/web-app_2_3.dtd">
<web-app>

  <display-name>Legacy Banking Service</display-name>

  <!-- Spring context loader -->
  <listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
  </listener>

  <!-- JAX-WS RI context listener: reads sun-jaxws.xml -->
  <listener>
    <listener-class>com.sun.xml.ws.transport.http.servlet.WSServletContextListener</listener-class>
  </listener>

  <!-- JAX-WS RI servlet: handles /ws/* requests -->
  <servlet>
    <servlet-name>JAXWSServlet</servlet-name>
    <servlet-class>com.sun.xml.ws.transport.http.servlet.WSServlet</servlet-class>
    <load-on-startup>1</load-on-startup>
  </servlet>

  <servlet-mapping>
    <servlet-name>JAXWSServlet</servlet-name>
    <url-pattern>/ws/*</url-pattern>
  </servlet-mapping>

</web-app>
```

Notes:
- **No** `contextConfigLocation` `<context-param>` needed — Spring defaults to `/WEB-INF/applicationContext.xml`.
- Old DTD DOCTYPE instead of XSD schema — authentic legacy.

---

## 3. `sun-jaxws.xml` — JAX-WS RI endpoint descriptor

```xml
<?xml version="1.0" encoding="UTF-8"?>
<endpoints xmlns="http://java.sun.com/xml/ns/jax-ws/ri/runtime"
           version="2.0">

  <endpoint
    name="BankingService"
    implementation="com.legacybank.service.BankingServiceImpl"
    url-pattern="/ws/banking"/>

</endpoints>
```

Key points:
- `implementation` must be the fully-qualified class name of the `@WebService` impl class.
- `url-pattern` must start with `/ws/` to match the servlet mapping in `web.xml`.
- Accessing `/ws/banking?wsdl` on the running Tomcat will return the auto-generated WSDL.

---

## 4. `applicationContext.xml` — Spring 3.2 XML bean configuration

Pure XML config — no `@Component`, `@Autowired`, or `@Configuration`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
         http://www.springframework.org/schema/beans
         http://www.springframework.org/schema/beans/spring-beans-3.2.xsd">

  <!-- Database connection properties -->
  <bean id="databaseManager" class="com.legacybank.service.DatabaseManager">
    <property name="jdbcUrl"   value="jdbc:postgresql://${DB_HOST:localhost}:5432/bankdb"/>
    <property name="username"  value="${DB_USER:bankuser}"/>
    <property name="password"  value="${DB_PASSWORD:bankpassword}"/>
  </bean>

  <!-- Service implementation — Spring manages the instance,
       JAX-WS RI calls SpringBeanAutowiringSupport to retrieve it -->
  <bean id="bankingServiceImpl" class="com.legacybank.service.BankingServiceImpl">
    <property name="databaseManager" ref="databaseManager"/>
  </bean>

</beans>
```

Notes:
- `${DB_HOST:localhost}` uses Spring's `PropertyPlaceholderConfigurer`-style syntax.
- No `<context:property-placeholder>` needed — the defaults in `${...:default}` cover local dev, Docker Compose injects overrides via environment variables that Spring's `SystemPropertiesPropertySource` picks up automatically in Spring 3.2.
- Actually for JAX-WS RI + Spring integration we need `SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this)` in the `@WebService` impl constructor, OR we use a `SpringBeanAutowiringInterceptor`. The simplest old-school approach is manually looking up the Spring context from `ServletContext` — see Plan 04 for the impl class detail.

---

## Implementation notes

- All four files are interdependent: `web.xml` references class names that must exist, `sun-jaxws.xml` references the impl class, `applicationContext.xml` references both service bean classes.
- Write them in the order above so references can be cross-checked.
- The WAR final name `banking-service` means Tomcat deploys it at context path `/banking-service`.
