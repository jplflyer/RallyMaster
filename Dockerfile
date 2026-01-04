# Simple runtime image for pre-built RallyServer JAR
# Build the JAR first with: ./gradlew :RallyServer:bootJar
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the pre-built JAR from local build
COPY RallyServer/build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
