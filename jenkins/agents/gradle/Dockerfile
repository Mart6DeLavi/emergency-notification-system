# Gradle build agent: JDK 21 (from the base image) + Docker CLI to build images
# through the host daemon (/var/run/docker.sock).
FROM jenkins/inbound-agent:jdk21

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        docker.io \
        git \
        curl \
        ca-certificates \
        unzip \
    && rm -rf /var/lib/apt/lists/*

USER jenkins
