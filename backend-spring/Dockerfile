# ==============================================================================
# Multi-stage Dockerfile for Spring Boot (Java 21)
# Production Container for AI College Life Copilot Backend
# ==============================================================================

# Stage 1: Build stage with Eclipse Temurin JDK 21 and Maven Wrapper
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Maven Wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Resolve project dependencies offline
RUN ./mvnw dependency:go-offline -B

# Copy project source code
COPY src ./src

# Package production executable JAR without running tests
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user setup for container security
RUN groupadd -r spring && useradd -r -g spring spring

# Copy pre-packaged executable JAR from build stage
COPY --from=build --chown=spring:spring /app/target/backend-spring-0.0.1-SNAPSHOT.jar app.jar

# Run container as non-root user
USER spring:spring

# Default port configuration (Render dynamically assigns $PORT)
ENV PORT=8080
EXPOSE 8080

# Conservative container-aware JVM memory tuning for Render 512MB RAM constraint:
# - MaxRAMPercentage=70.0 limits heap to ~358MB, reserving ~154MB for native memory & metaspace
# - ExitOnOutOfMemoryError ensures immediate container restart on OOM
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=40.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
