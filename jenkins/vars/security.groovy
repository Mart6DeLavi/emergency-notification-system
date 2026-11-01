def security(String service) {
    def image = "${env.DOCKER_IMAGE_PREFIX}/${service}:${env.BUILD_NUMBER}"

    // Blocking gate: OS/base-image vulnerabilities only.
    sh """
        docker run --rm \
            -v /var/run/docker.sock:/var/run/docker.sock \
            -v trivy_cache_${service}:/root/.cache \
            aquasec/trivy:latest image \
            --pkg-types os \
            --severity HIGH,CRITICAL \
            --ignore-unfixed \
            --exit-code 1 \
            ${image}
    """

    // Report-only: application/library vulnerabilities (app.jar).
    sh """
        docker run --rm \
            -v /var/run/docker.sock:/var/run/docker.sock \
            -v trivy_cache_${service}:/root/.cache \
            aquasec/trivy:latest image \
            --pkg-types library \
            --severity HIGH,CRITICAL \
            --ignore-unfixed \
            --exit-code 0 \
            ${image}
    """
}

return this
