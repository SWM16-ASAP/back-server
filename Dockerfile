FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

# Copy pre-built JAR file from CI/CD
COPY build/libs/*.jar app.jar
COPY infra/performance-test/docker/app-entrypoint.sh app-entrypoint.sh

RUN chmod +x app-entrypoint.sh

EXPOSE 8080

# Run the application
ENTRYPOINT ["/app/app-entrypoint.sh"]
