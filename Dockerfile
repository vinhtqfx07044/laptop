# Use official Eclipse Temurin Java 21 runtime as base image (Alpine for smaller size)
FROM eclipse-temurin:21-jdk-alpine AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies (better layer caching)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Runtime stage - use smaller JRE image
FROM eclipse-temurin:21-jre-alpine

# Create app directory
WORKDIR /app

# Create uploads directory for file uploads
RUN mkdir -p uploads logs

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Railway uses PORT environment variable, default to 8080)
EXPOSE 8080

# Run the application with production profile and Railway environment variables
CMD java -Xms256m -Xmx512m -Dspring.profiles.active=prod -jar app.jar