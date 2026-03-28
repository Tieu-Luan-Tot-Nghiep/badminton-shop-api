FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# The jar is built by Jenkins in previous stages.
COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
