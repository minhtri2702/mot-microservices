FROM openjdk:21-jdk-slim AS build

WORKDIR /app

# Copy Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY commons/pom.xml commons/
COPY product-services/pom.xml product-services/

# Copy source code
COPY commons/src commons/src
COPY product-services/src product-services/src

# Build the product-services module
RUN ./mvnw clean package -pl product-services -am -DskipTests

# Runtime stage
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/product-services/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
