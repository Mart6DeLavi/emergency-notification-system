FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN mkdir -p /app/models && \
    curl -L -o /app/models/nsfw-detection-model.onnx \
    https://huggingface.co/onnx-community/nsfw_image_detection-ONNX/resolve/main/onnx/model.onnx || \
    echo "NSFW model download skipped (optional)"

COPY --from=builder /app/build/libs/*.jar /app/app.jar

EXPOSE 8002

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
