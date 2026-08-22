FROM rust:1.98-slim AS builder

RUN apt-get update && apt-get install -y pkg-config libssl-dev librdkafka-dev && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .
RUN cargo build --release

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y ca-certificates librdkafka1 libssl3 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/target/release/message-delivery-service /usr/local/bin/message-delivery-service

EXPOSE 8005
ENTRYPOINT ["message-delivery-service"]
