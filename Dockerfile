# Dockerfile for Spring Boot app (Java 21)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built jar from build/libs (Gradle default)
ARG JAR_PATH=build/libs/*.jar
COPY ${JAR_PATH} app.jar

EXPOSE 80

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
