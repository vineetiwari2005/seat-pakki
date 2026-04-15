# syntax=docker/dockerfile:1

# Stage 1: Build the application JAR
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy build files first for better layer caching
COPY pom.xml ./

# Download dependencies early
RUN mvn -q -DskipTests dependency:go-offline

# Copy source and package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run with a lightweight JRE
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
