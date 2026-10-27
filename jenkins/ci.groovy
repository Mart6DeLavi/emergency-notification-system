// Thin aggregator for the CI stages.
//
// The root Jenkinsfile only needs:
//   def ci = load 'jenkins/ci.groovy'
// and then calls ci.quality|build|security|deploy|notify('<service>').
//
// Each stage lives in its own file under jenkins/vars/ so that changes to a
// single stage are easy to introduce without touching the others.

def services = load 'jenkins/services.groovy'
def qualityScript = load 'jenkins/vars/quality.groovy'
def buildScript = load 'jenkins/vars/build.groovy'
def securityScript = load 'jenkins/vars/security.groovy'
def deployScript = load 'jenkins/vars/deploy.groovy'
def notifyScript = load 'jenkins/vars/notify.groovy'

def quality(String service) {
    qualityScript.quality(service, services.typeOf(service))
}

def build(String service) {
    buildScript.build(service, services.typeOf(service))
}

def security(String service) {
    securityScript.security(service)
}

def deploy(String service) {
    deployScript.deploy(service)
}

def rolloutAll() {
    deployScript.rolloutAll()
}

def notify(String result) {
    notifyScript.notify(result)
}

def allServices() {
    return services.allServices()
}

def agentLabel(String service) {
    return services.agentLabel(service)
}

return this
