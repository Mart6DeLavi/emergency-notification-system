pipeline {
    agent none

    environment {
        REGISTRY = credentials('docker-registry-credentials')
        DOCKER_IMAGE_PREFIX = 'sensa'
    }

    stages {
        stage('Build & Test — Java/Kotlin (Gradle)') {
            parallel {
                stage('authentication-service') {
                    agent { label 'gradle' }
                    steps { gradleBuild('authentication-service') }
                }
                stage('discovery-server') {
                    agent { label 'gradle' }
                    steps { gradleBuild('discovery-server') }
                }
                stage('filesystem-service') {
                    agent { label 'gradle' }
                    steps { gradleBuild('filesystem-service') }
                }
                stage('notification-service') {
                    agent { label 'gradle' }
                    steps { gradleBuild('notification-service') }
                }
                stage('template-service') {
                    agent { label 'gradle' }
                    steps { gradleBuild('template-service') }
                }
                stage('user-management-service') {
                    agent { label 'gradle' }
                    steps { gradleBuild('user-management-service') }
                }
            }
        }

        stage('Build & Test — Rust (Cargo)') {
            parallel {
                stage('emergency-situation-request-service') {
                    agent { label 'rust' }
                    steps { cargoBuild('emergency-situation-request-service') }
                }
                stage('message-delivery-service') {
                    agent { label 'rust' }
                    steps { cargoBuild('message-delivery-service') }
                }
                stage('load-balancer-service') {
                    agent { label 'rust' }
                    steps { cargoBuild('load-balancer-service') }
                }
                stage('rate-limiter-service') {
                    agent { label 'rust' }
                    steps { cargoBuild('rate-limiter-service') }
                }
                stage('api-gateway') {
                    agent { label 'rust' }
                    steps { cargoBuild('api-gateway') }
                }
            }
        }

        stage('Docker Build & Push') {
            agent { label 'docker' }
            steps {
                script {
                    def services = [
                        'authentication-service',
                        'discovery-server',
                        'filesystem-service',
                        'notification-service',
                        'template-service',
                        'user-management-service',
                        'emergency-situation-request-service',
                        'message-delivery-service',
                        'load-balancer-service',
                        'rate-limiter-service',
                        'api-gateway'
                    ]
                    services.each { service ->
                        dockerBuildAndPush(service)
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}

def gradleBuild(String service) {
    sh """
        cd ${service}
        ./gradlew --no-daemon clean build
    """
}

def cargoBuild(String service) {
    sh """
        cd ${service}
        cargo build
        cargo test
    """
}

def dockerBuildAndPush(String service) {
    def imageName = "${DOCKER_IMAGE_PREFIX}/${service}"
    def versionTag = "${env.BUILD_NUMBER}"
    sh """
        docker build -t ${imageName}:${versionTag} -t ${imageName}:latest ./${service}
        docker push ${imageName}:${versionTag}
        docker push ${imageName}:latest
    """
}
