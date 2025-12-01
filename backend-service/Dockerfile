# Multi-stage build for Spring Boot backend
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom and download dependencies (layer caching)
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -q -DskipTests package

# Runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built jar
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Set environment defaults (overridable in compose)
ENV API_BASE_URL=http://python-service:8000 \
    JAVA_OPTS=""

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]