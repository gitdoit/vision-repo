# Multi-stage build for vision-admin
# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime with FFmpeg
FROM eclipse-temurin:17-jre-jammy

# Install FFmpeg
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r vision && useradd -r -g vision vision

# Create directories
RUN mkdir -p /data/vision/files /data/vision/models /tmp/vision-capture && \
    chown -R vision:vision /data /tmp/vision-capture

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/vision-admin-*.jar app.jar

# Change ownership
RUN chown vision:vision app.jar

# Switch to non-root user
USER vision

# Expose port
EXPOSE 26330

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:26330/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
