// SENSA CI/CD pipeline.
//
// Every microservice runs four stages in parallel:
//   Quality  -> run tests
//   Build    -> build artifact + Docker image
//   Security -> blocking Trivy scan (HIGH/CRITICAL)
//   Deploy   -> push image to registry (+ optional guarded rollout)
//
// All stage logic lives in jenkins/vars/*.groovy and is aggregated by
// jenkins/ci.groovy. The list of services lives in jenkins/services.groovy.

env.DOCKER_IMAGE_PREFIX = env.DOCKER_IMAGE_PREFIX ?: 'sensa'
env.NOTIFY_ENABLED = env.NOTIFY_ENABLED ?: 'false'
env.NOTIFY_EMAIL_RECIPIENTS = env.NOTIFY_EMAIL_RECIPIENTS ?: ''
env.DEPLOY_HOST = env.DEPLOY_HOST ?: ''

// Bootstrap on any node: checkout once to read the central service list.
def labels = [:]
node {
    checkout scm
    def bootstrap = load 'jenkins/services.groovy'
    bootstrap.allServices().each { service ->
        labels[service] = bootstrap.agentLabel(service)
    }
}

try {
    stage('Services') {
        def branches = labels.collectEntries { service, label ->
            [(service): {
                node(label) {
                    checkout scm
                    def ci = load 'jenkins/ci.groovy'
                    try {
                        stage(service) {
                            stage('Quality')  { ci.quality(service) }
                            stage('Build')    { ci.build(service) }
                            stage('Security') { ci.security(service) }
                            stage('Deploy')   { ci.deploy(service) }
                        }
                    } finally {
                        cleanWs()
                    }
                }
            }]
        }
        parallel branches
    }
} finally {
    node {
        checkout scm
        def ci = load 'jenkins/ci.groovy'
        ci.notify(currentBuild.currentResult)
    }
}
