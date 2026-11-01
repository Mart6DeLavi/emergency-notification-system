# syntax=docker/dockerfile:1.7
FROM rust:1.98-slim-bookworm AS builder

RUN apt-get update && apt-get install -y pkg-config libssl-dev && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .
RUN --mount=type=cache,id=cargo-target-message-delivery-service,target=/app/target --mount=type=cache,id=cargo-registry,target=/usr/local/cargo/registry cargo build --release && cp /app/target/release/message-delivery-service /app/message-delivery-service

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y ca-certificates libssl3 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/message-delivery-service /usr/local/bin/message-delivery-service

EXPOSE 8005
ENTRYPOINT ["message-delivery-service"]
