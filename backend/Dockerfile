# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# pom primeiro: enquanto as dependencias nao mudam, esta camada vem do cache
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Execucao
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
# MaxRAMPercentage: a JVM respeita o limite de memoria do container em vez de
# achar que a maquina inteira e dela.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
