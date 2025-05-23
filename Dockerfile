FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven build files first for caching
COPY pom.xml .
COPY src ./src
COPY .mvn .mvn
COPY mvnw .

# Package the application
RUN chmod +x mvnw && ./mvnw clean package

# Use a slim runtime image for final container
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/target/exchanger-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]