// Stage 2 — Build: produce the artifact and the Docker image.
// Tests are NOT re-run here (they already passed in the Quality stage).

def build(String service, String type) {
    if (type == 'gradle') {
        sh "cd ${service} && ./gradlew --no-daemon assemble"
    } else if (type == 'rust') {
        sh "cd ${service} && cargo build --release"
    } else {
        error "build: unknown service type '${type}' for service '${service}'"
    }

    def image = "${env.DOCKER_IMAGE_PREFIX}/${service}"
    sh """
        docker build -t ${image}:${env.BUILD_NUMBER} -t ${image}:latest ./${service}
    """
}

return this
