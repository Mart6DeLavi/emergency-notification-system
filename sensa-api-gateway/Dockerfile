FROM eclipse-temurin:21-jdk

COPY build/libs/*.jar /app.jar
EXPOSE 8007
ENTRYPOINT ["java", "-jar", "/app.jar"]