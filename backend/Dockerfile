FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser && mkdir -p /app/data && chown -R appuser:appuser /app
COPY --from=build --chown=appuser:appuser /src/target/pedidos-agreste-api.jar /app/app.jar
USER appuser
ENV SPRING_PROFILES_ACTIVE=h2 DB_FILE=/app/data/pedidos PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
