# ============================================
# Dockerfile cho product-services (Spring Boot)
# ============================================
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy parent POM
COPY pom.xml ./
COPY commons/pom.xml ./commons/
COPY product-services/pom.xml ./product-services/

# Copy source code
COPY commons/src ./commons/src/
COPY product-services/src ./product-services/src/

# Build (skip tests for faster build)
RUN mvn clean package -pl product-services -am -DskipTests -q

# ============================================
# Run stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy built JAR
COPY --from=build /app/product-services/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
