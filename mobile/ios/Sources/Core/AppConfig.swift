import Foundation

enum AppConfig {
    /// Production API gateway. The gateway exposes `/api/v1/...`; Caddy maps
    /// `https://panel.sensanotify.com/api/*` to the gateway.
    ///
    /// For local development set the `API_BASE_URL` environment variable in the
    /// Xcode scheme (for example `http://localhost:8080`).
    static var apiBaseURL: URL {
        if let raw = ProcessInfo.processInfo.environment["API_BASE_URL"], !raw.isEmpty {
            let trimmed = raw.hasSuffix("/") ? String(raw.dropLast()) : raw
            if let url = URL(string: trimmed) {
                return url
            }
        }
        return URL(string: "https://panel.sensanotify.com/api")!
    }
}
