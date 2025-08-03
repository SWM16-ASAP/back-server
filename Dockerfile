FROM openjdk:17

WORKDIR /app

# Copy pre-built JAR file from CI/CD
COPY build/libs/*.jar app.jar

EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]