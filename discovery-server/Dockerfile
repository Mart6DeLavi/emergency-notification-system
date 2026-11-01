# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradle ./gradle
COPY gradlew ./
COPY build.gradle* settings.gradle* ./
COPY src ./src

RUN --mount=type=cache,id=gradle-discovery-server,target=/root/.gradle chmod +x gradlew && ./gradlew --no-daemon clean build -x test && rm -f build/libs/*-plain.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
