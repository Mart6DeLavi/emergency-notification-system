import Foundation

enum AppConfig {
    /// Production API gateway. The gateway exposes `/api/v1/...`; Caddy maps
    /// `https://panel.sensanotify.com/api/*` to the gateway.
    ///
    /// For local development change this constant (or add a dedicated
    /// non-production override). Do not read `API_BASE_URL` from the process
    /// environment: launch environments (including Appetize) can inject a
    /// relative or malformed value, which makes URLSession fail with
    /// `NSURLErrorBadURL`.
    static let apiBaseURL = URL(string: "https://panel.sensanotify.com/api")!
}
