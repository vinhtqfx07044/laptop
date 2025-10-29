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

# Create uploads directory for file uploads and logs
RUN mkdir -p images documents logs && chmod 755 images documents logs

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Railway uses PORT environment variable, default to 8080)
EXPOSE 8080

# Add health check for Railway to detect when app is ready
# Spring Boot Actuator health endpoint must be enabled
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run the application with production profile and Railway environment variables
# Use PORT from Railway if available, otherwise default to 8080
CMD ["java", "-Xms256m", "-Xmx512m", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]