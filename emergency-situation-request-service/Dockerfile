FROM rust:1.98-slim AS builder

RUN apt-get update && apt-get install -y --no-install-recommends \
    pkg-config libssl-dev librdkafka-dev && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY Cargo.toml ./
COPY migrations ./migrations
COPY src ./src

RUN cargo build --release

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates librdkafka1 libssl3 && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/release/emergency-situation-request-service /usr/local/bin/
COPY --from=builder /app/migrations /app/migrations

EXPOSE 8006
ENTRYPOINT ["emergency-situation-request-service"]
