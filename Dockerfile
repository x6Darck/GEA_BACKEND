# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from build /app/target/*.jar app.jar

# Configuration
ENV SERVER_PORT=8080
ENV DB_URL=jdbc:mysql://mysql:3306/eventos_institucionales
ENV DB_USER=root
ENV DB_PASS=password
ENV JWT_SECRET=change_me_in_production_12345678901234567890123456789012

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
