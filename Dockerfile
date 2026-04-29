# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src
# Build the application skipping tests to speed up deployment
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar
# Expose port 8080 (Render provides the PORT environment variable)
EXPOSE 8080
# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
