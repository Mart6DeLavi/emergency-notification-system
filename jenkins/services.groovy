// Single source of truth for the list of services built by CI.
// Keep in sync with the module list in the root settings.gradle.
//
// Loaded from jenkins/ci.groovy via the `load` step, so the returned
// object exposes the methods below (call with `services.isGradle('x')` etc.).

def gradleServices() {
    return [
        'authentication-service',
        'discovery-server',
        'filesystem-service',
        'notification-service',
        'template-service',
        'user-management-service'
    ]
}

def rustServices() {
    return [
        'emergency-situation-request-service',
        'message-delivery-service',
        'load-balancer-service',
        'rate-limiter-service',
        'api-gateway'
    ]
}

def allServices() {
    return gradleServices() + rustServices()
}

def isGradle(String service) {
    return gradleServices().contains(service)
}

def isRust(String service) {
    return rustServices().contains(service)
}

def typeOf(String service) {
    if (isGradle(service)) {
        return 'gradle'
    }
    if (isRust(service)) {
        return 'rust'
    }
    return 'unknown'
}

// Jenkins agent label used to build this service.
def agentLabel(String service) {
    return typeOf(service) == 'gradle' ? 'gradle' : 'rust'
}

return this
