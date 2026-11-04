FROM rust:1.98-slim-bookworm AS builder

RUN apt-get update && apt-get install -y --no-install-recommends \
    pkg-config libssl-dev && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY Cargo.toml ./
COPY src ./src
RUN cargo build --release

FROM grafana/k6:0.54.0 AS k6

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates libssl3 && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/release/test-lab-service /usr/local/bin/test-lab-service
COPY --from=k6 /usr/bin/k6 /usr/local/bin/k6
COPY k6/scenario.js /app/k6/scenario.js

ENV K6_BIN=/usr/local/bin/k6
ENV K6_SCRIPT=/app/k6/scenario.js
EXPOSE 8010
ENTRYPOINT ["test-lab-service"]
