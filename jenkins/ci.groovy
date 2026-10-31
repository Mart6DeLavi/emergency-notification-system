// Thin aggregator for the CI stages.
//
// The root Jenkinsfile only needs:
//   def ci = load 'jenkins/ci.groovy'
// and then calls ci.quality|build|security|deploy('<service>') plus
// ci.rolloutAll() and ci.notify(result).
//
// Each stage lives in its own file under jenkins/vars/ so that changes to a
// single stage are easy to introduce without touching the others.
//
// NOTE: loaded scripts are fetched inside each method on purpose. Top-level
// `def` variables in a loaded Groovy script are locals of run() and are not
// visible from methods, which produced MissingPropertyException in build #1.

def quality(String service) {
    def services = load 'jenkins/services.groovy'
    def stage = load 'jenkins/vars/quality.groovy'
    stage.quality(service, services.typeOf(service))
}

def build(String service) {
    def services = load 'jenkins/services.groovy'
    def stage = load 'jenkins/vars/build.groovy'
    stage.build(service, services.typeOf(service))
}

def security(String service) {
    def stage = load 'jenkins/vars/security.groovy'
    stage.security(service)
}

def deploy(String service) {
    def stage = load 'jenkins/vars/deploy.groovy'
    stage.deploy(service)
}

def rolloutAll() {
    def stage = load 'jenkins/vars/deploy.groovy'
    stage.rolloutAll()
}

def notify(String result) {
    def stage = load 'jenkins/vars/notify.groovy'
    stage.notify(result)
}

def allServices() {
    def services = load 'jenkins/services.groovy'
    return services.allServices()
}

def agentLabel(String service) {
    def services = load 'jenkins/services.groovy'
    return services.agentLabel(service)
}

return this
