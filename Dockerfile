FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B && mv target/reconpay-*.jar target/app.jar

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

RUN addgroup -S reconpay && adduser -S -G reconpay reconpay

COPY --from=build --chown=reconpay:reconpay /app/target/app.jar app.jar

USER reconpay

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
