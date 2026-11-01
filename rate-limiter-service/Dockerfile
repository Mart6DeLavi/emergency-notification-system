# syntax=docker/dockerfile:1.7
FROM rust:1.98-slim-bookworm AS builder

RUN apt-get update && apt-get install -y --no-install-recommends \
    pkg-config && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY Cargo.toml ./
COPY config.yaml ./
COPY src ./src

RUN --mount=type=cache,id=cargo-target-rate-limiter-service,target=/app/target --mount=type=cache,id=cargo-registry,target=/usr/local/cargo/registry cargo build --release && cp /app/target/release/rate-limiter-service /app/rate-limiter-service

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/rate-limiter-service /usr/local/bin/
COPY --from=builder /app/config.yaml /app/config.yaml

WORKDIR /app
EXPOSE 8009
ENTRYPOINT ["rate-limiter-service"]
