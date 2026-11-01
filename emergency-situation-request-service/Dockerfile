# syntax=docker/dockerfile:1.7
FROM rust:1.98-slim-bookworm AS builder

RUN apt-get update && apt-get install -y --no-install-recommends \
    pkg-config libssl-dev cmake build-essential libsasl2-dev libzstd-dev libcurl4-openssl-dev zlib1g-dev && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY Cargo.toml ./
COPY migrations ./migrations
COPY src ./src

RUN --mount=type=cache,id=cargo-target-emergency-situation-request-service,target=/app/target --mount=type=cache,id=cargo-registry,target=/usr/local/cargo/registry cargo build --release

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates libssl3 libsasl2-2 libzstd1 libcurl4 zlib1g && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/release/emergency-situation-request-service /usr/local/bin/
COPY --from=builder /app/migrations /app/migrations

EXPOSE 8006
ENTRYPOINT ["emergency-situation-request-service"]
