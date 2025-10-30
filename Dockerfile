# syntax=docker/dockerfile:1

### Build stage ###
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

### Runtime stage ###
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cài curl cho healthcheck (chạy dưới root)
RUN apk add --no-cache curl

# Tạo thư mục cần thiết
RUN mkdir -p /app/images /app/documents /app/logs

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV PORT=8080 JAVA_OPTS="-Xms256m -Xmx512m"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl --fail http://127.0.0.1:${PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]