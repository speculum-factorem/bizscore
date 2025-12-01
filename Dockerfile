# Multi-stage build for optimized Docker image
FROM eclipse-temurin:21-jdk as builder
WORKDIR application
COPY target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers

FROM eclipse-temurin:21-jre
RUN addgroup --system --gid 1000 appuser && \
    adduser --system --uid 1000 --group appuser

USER appuser
WORKDIR /app

# Copy layers from builder stage
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./

# Create logs directory with proper permissions
# Install curl for healthcheck
USER root
RUN apt-get update && apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /app/logs && \
    chown appuser:appuser /app/logs
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for production
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]