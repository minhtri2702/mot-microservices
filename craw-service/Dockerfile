FROM eclipse-temurin:17-jdk-jammy


WORKDIR /app

COPY target/craw-service-4.0.2.jar app.jar

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]

