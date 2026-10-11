// Stage 1 — Quality: run the test suite only.
// Tests are the gate: if they fail, Build/Security/Deploy never run for this service.

def quality(String service, String type) {
    if (type == 'gradle') {
        sh "cd ${service} && ./gradlew --no-daemon test"
    } else if (type == 'rust') {
        sh "cd ${service} && cargo test"
    } else {
        error "quality: unknown service type '${type}' for service '${service}'"
    }
}

return this
