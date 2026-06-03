FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

COPY commons commons
COPY account-service account-service
COPY product-services product-services

RUN mvn clean package \
    -pl account-service \
    -am \
    -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/account-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=docker"]
