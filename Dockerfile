# Stage 1: build
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/expensapp_api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
