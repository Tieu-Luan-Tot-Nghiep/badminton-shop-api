FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ARG JAR_PATH=build/libs/*.jar
COPY ${JAR_PATH} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
