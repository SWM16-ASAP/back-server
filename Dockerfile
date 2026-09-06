FROM amazoncorretto:17-alpine-jdk AS runtime

WORKDIR /app

# Copy pre-built JAR file from CI/CD
COPY build/libs/*.jar app.jar

EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM runtime AS performance-test

ADD --checksum=sha256:bbf83c151b6400709e2f225bdd07a04f839d9d13b8b93464241333fd25d3e3ba \
	https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.31.1/opentelemetry-javaagent.jar \
	/opt/opentelemetry-javaagent.jar

FROM runtime AS production
