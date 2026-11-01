# Gradle build agent: JDK 21 (from the base image) + Docker CLI to build images
# through the host daemon (/var/run/docker.sock).
FROM jenkins/inbound-agent:jdk21

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        git \
        curl \
        ca-certificates \
        unzip \
        openssh-client \
    && rm -rf /var/lib/apt/lists/*

# Docker CLI only. The Debian `docker.io` package ships the daemon, not the
# client, so install the official static client binary.
ARG DOCKER_CLI_VERSION=27.5.1
RUN curl -fsSL "https://download.docker.com/linux/static/stable/x86_64/docker-${DOCKER_CLI_VERSION}.tgz" \
    | tar -xz --strip-components=1 -C /usr/local/bin docker/docker \
    && docker --version

USER root
RUN mkdir -p /home/jenkins/.gradle && chown -R jenkins:jenkins /home/jenkins
RUN mkdir -p /usr/local/lib/docker/cli-plugins \
    && curl -fsSL "https://github.com/docker/buildx/releases/download/v0.19.3/buildx-v0.19.3.linux-amd64" \
       -o /usr/local/lib/docker/cli-plugins/docker-buildx \
    && chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx \
    && docker buildx version
USER jenkins
ENV GRADLE_USER_HOME=/home/jenkins/.gradle
ENV DOCKER_BUILDKIT=1
