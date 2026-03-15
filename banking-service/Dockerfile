FROM maven:3.6.3-jdk-8 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -B -DskipTests

FROM tomcat:7.0.109-jdk8

RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=builder /build/target/banking-service.war /usr/local/tomcat/webapps/banking-service.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
