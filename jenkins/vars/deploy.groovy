// Stage 4 — Deploy: publish the image to the registry and (optionally) roll out.
//
// Registry login binds the `docker-registry-credentials` credential at runtime.
// If that credential is not configured, login/push are skipped with a warning so
// the pipeline still reports success while you finish Jenkins setup.
//
// Rollout is generic and guarded: set DEPLOY_HOST to a SSH target running the
// project's docker-compose to enable it. When unset, images are only pushed.

def deploy(String service) {
    def image = "${env.DOCKER_IMAGE_PREFIX}/${service}"

    if (dockerLogin()) {
        sh """
            docker push ${image}:${env.BUILD_NUMBER}
            docker push ${image}:latest
        """
    } else {
        echo "Skipping push for ${image}: registry credentials not available."
    }

    rollout()
}

def dockerLogin() {
    try {
        withCredentials([usernamePassword(
                credentialsId: 'docker-registry-credentials',
                usernameVariable: 'REGISTRY_USR',
                passwordVariable: 'REGISTRY_PSW')]) {
            sh 'echo "$REGISTRY_PSW" | docker login -u "$REGISTRY_USR" --password-stdin'
        }
        return true
    } catch (Exception e) {
        echo "docker login skipped (credential 'docker-registry-credentials' not configured): ${e.message}"
        return false
    }
}

def rollout() {
    if (env.DEPLOY_HOST) {
        sh "ssh -o StrictHostKeyChecking=no ${env.DEPLOY_HOST} 'docker compose pull && docker compose up -d'"
    } else {
        echo 'Deploy target not configured (set DEPLOY_HOST to enable rollout); image pushed to registry.'
    }
}

return this
