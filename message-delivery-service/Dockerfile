FROM eclipse-temurin:21-jdk

COPY build/libs/*.jar /app.jar

EXPOSE 8005

ENTRYPOINT ["java", "-jar", "/app.jar"]
