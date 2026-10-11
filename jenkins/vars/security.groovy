// Stage 3 — Security: blocking vulnerability scan of the freshly built image.
// Build goes red on any HIGH/CRITICAL finding.

def security(String service) {
    def image = "${env.DOCKER_IMAGE_PREFIX}/${service}:${env.BUILD_NUMBER}"
    sh """
        docker run --rm \
            -v /var/run/docker.sock:/var/run/docker.sock \
            -v trivy_cache:/root/.cache \
            aquasec/trivy:latest image \
            --severity HIGH,CRITICAL \
            --exit-code 1 \
            ${image}
    """
}

return this
