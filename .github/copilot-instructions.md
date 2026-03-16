# Project Guidelines

## Architecture
- Workspace contains two applications:
- banking-service: legacy Java 8 style backend with Spring 3.2 XML config, JAX-WS SOAP endpoint, raw JDBC access to PostgreSQL.
- banking-client: legacy Java Swing desktop app using generated SOAP stubs from a local WSDL copy.
- Runtime orchestration is docker compose with postgres:9.3 and banking-service on Tomcat 7.
- SOAP endpoint path is http://localhost:8080/banking-service/banking and WSDL path is http://localhost:8080/banking-service/banking?wsdl.

## Build and Run
- Backend build: cd banking-service && mvn clean package
- Client build: cd banking-client && mvn clean package -DskipTests
- Start backend stack: docker compose up -d --build
- Check backend status: docker compose ps
- Check backend logs: docker compose logs banking-service --tail=200
- Run client jar on Windows without terminal: javaw -jar banking-client/target/banking-client-1.0.jar

## Conventions
- Preserve legacy style in backend:
- Spring XML wiring in WEB-INF/applicationContext.xml.
- JAX-WS endpoint descriptors in WEB-INF/web.xml and WEB-INF/sun-jaxws.xml.
- Direct SQL with PreparedStatement in DatabaseManager; no JPA/Hibernate.
- Preserve legacy Swing style in client:
- GridBagLayout and classic panels.
- Anonymous ActionListener classes.
- DefaultTableModel with Vector-based table rows.
- Keep client WSDL in sync with service contract:
- If backend BankingService interface changes, update banking-client/src/main/resources/wsdl/banking.wsdl and rebuild client to regenerate stubs.

## Compatibility Notes
- Keep runtime dependencies legacy unless a host-toolchain compatibility fix is required.
- maven-war-plugin must remain 3.x+ for backend builds on modern Maven/JDK.
- Client startup uses local classpath WSDL metadata and then overrides endpoint URL at runtime.
- Do not change seed account numbers in db/init.sql unless requested; UI login examples rely on them.

## Safety and Pitfalls
- Do not replace SOAP with REST in this project unless explicitly requested.
- Do not modernize to Spring Boot unless explicitly requested.
- Do not remove hardcoded legacy endpoint/timeouts without adding replacement configuration.
- For login failures, first verify endpoint URL and backend container health before changing client logic.
