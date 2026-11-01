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
env.DEPLOY_HOST = env.DEPLOY_HOST ?: 'host.docker.internal'
env.DEPLOY_PATH = env.DEPLOY_PATH ?: '/opt/sensa'
env.PANEL_DOMAIN = env.PANEL_DOMAIN ?: 'panel.sensanotify.com'

// Bootstrap on any node: checkout once to read the central service list.
def labels = [:]
stage('Context') {
    node {
        checkout scm
        def commit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
        def branch = env.BRANCH_NAME ?: 'master'
        currentBuild.description = "branch=${branch} commit=${commit} tag=${env.BUILD_NUMBER}"
        echo "Context: branch=${branch}, commit=${commit}, build=${env.BUILD_NUMBER}, image=${env.DOCKER_IMAGE_PREFIX}:${env.BUILD_NUMBER}, panel=https://${env.PANEL_DOMAIN}"
        def bootstrap = load 'jenkins/services.groovy'
        bootstrap.allServices().each { service ->
            labels[service] = bootstrap.agentLabel(service)
        }
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

    stage('Rollout') {
        node('rust') {
            checkout scm
            try {
                def ci = load 'jenkins/ci.groovy'
                ci.rolloutAll()
            } finally {
                cleanWs()
            }
        }
    }
} finally {
    stage('Post Actions') {
        node {
            checkout scm
            def ci = load 'jenkins/ci.groovy'
            try {
                ci.notify(currentBuild.currentResult)
            } finally {
                ci.cleanup()
            }
        }
    }
}
