FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradle ./gradle
COPY gradlew ./
COPY build.gradle* settings.gradle* ./
COPY src ./src

RUN chmod +x gradlew && ./gradlew --no-daemon clean build -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8003

ENTRYPOINT ["java", "-jar", "app.jar"]
