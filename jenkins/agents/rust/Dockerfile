# Rust build agent: Docker CLI + native build deps for the Rust services
# (rdkafka with `dynamic-linking` needs librdkafka) + the Rust toolchain.

FROM jenkins/inbound-agent:jdk21

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        docker.io \
        git \
        curl \
        ca-certificates \
        build-essential \
        pkg-config \
        libssl-dev \
        librdkafka-dev \
    && rm -rf /var/lib/apt/lists/*

USER jenkins
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \
    | sh -s -- -y --default-toolchain 1.98.0 --profile minimal
ENV PATH="/home/jenkins/.cargo/bin:${PATH}"
