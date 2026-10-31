# Rust build agent: Docker CLI + native build deps for the Rust services
# (rdkafka with `dynamic-linking` needs librdkafka) + the Rust toolchain.

FROM jenkins/inbound-agent:jdk21

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        git \
        curl \
        ca-certificates \
        openssh-client \
        build-essential \
        pkg-config \
        libssl-dev \
        librdkafka-dev \
    && rm -rf /var/lib/apt/lists/*

# Docker CLI only. The Debian `docker.io` package ships the daemon, not the
# client, so install the official static client binary.
ARG DOCKER_CLI_VERSION=27.5.1
RUN curl -fsSL "https://download.docker.com/linux/static/stable/x86_64/docker-${DOCKER_CLI_VERSION}.tgz" \
    | tar -xz --strip-components=1 -C /usr/local/bin docker/docker \
    && docker --version

USER jenkins
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \
    | sh -s -- -y --default-toolchain 1.98.0 --profile minimal
ENV PATH="/home/jenkins/.cargo/bin:${PATH}"
