// Stage 4 — Deploy: publish the image to the registry (optional).
//
// Registry login binds the `docker-registry-credentials` credential at runtime.
// If that credential is not configured, login/push are skipped with a warning so
// the pipeline still reports success while you finish Jenkins setup.
//
// The actual rollout happens once, after all services are built and scanned,
// via rolloutAll() (see the Rollout stage in the root Jenkinsfile).

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

// Pull master on the deployment host and bring the stack up with the images
// that were just built on the shared Docker daemon (tagged with BUILD_NUMBER).
def rolloutAll() {
    if (!env.DEPLOY_HOST) {
        echo 'Deploy target not configured (set DEPLOY_HOST to enable rollout).'
        return
    }

    withCredentials([sshUserPrivateKey(
            credentialsId: 'sensa-deploy-ssh',
            usernameVariable: 'DEPLOY_USER',
            keyFileVariable: 'DEPLOY_KEY')]) {
        sh """
            ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \\
                -i "\$DEPLOY_KEY" "\$DEPLOY_USER@${env.DEPLOY_HOST}" \\
                "cd ${env.DEPLOY_PATH} && \\
                 git fetch origin master && \\
                 git reset --hard origin/master && \\
                 IMAGE_TAG=${env.BUILD_NUMBER} docker compose up -d && \\
                 IMAGE_TAG=${env.BUILD_NUMBER} docker compose up -d --no-deps --build rescue-panel"
        """
    }
}

return this
