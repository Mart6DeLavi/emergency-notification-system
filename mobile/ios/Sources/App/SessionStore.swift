import Foundation
import Combine

@MainActor
final class SessionStore: ObservableObject {
    @Published private(set) var token: String?
    @Published private(set) var userId: UUID?
    @Published private(set) var email: String?

    let api: APIClient

    private enum Keys {
        static let token = "sensa.jwt"
        static let userId = "sensa.userId"
        static let email = "sensa.email"
    }

    init() {
        api = APIClient(baseURL: AppConfig.apiBaseURL)

        token = KeychainStore.read(key: Keys.token)
        if let raw = UserDefaults.standard.string(forKey: Keys.userId) {
            userId = UUID(uuidString: raw)
        }
        email = UserDefaults.standard.string(forKey: Keys.email)
        api.token = token

        api.onUnauthorized = { [weak self] in
            Task { @MainActor [weak self] in
                self?.signOut()
            }
        }
    }

    var isAuthenticated: Bool {
        token != nil
    }

    func signIn(_ response: AuthResponse) {
        token = response.token
        userId = response.userId
        email = response.email

        KeychainStore.save(response.token, key: Keys.token)
        UserDefaults.standard.set(response.userId.uuidString, forKey: Keys.userId)
        UserDefaults.standard.set(response.email, forKey: Keys.email)
        api.token = response.token
    }

    func signOut() {
        token = nil
        userId = nil
        email = nil

        KeychainStore.delete(key: Keys.token)
        UserDefaults.standard.removeObject(forKey: Keys.userId)
        UserDefaults.standard.removeObject(forKey: Keys.email)
        api.token = nil
    }
}
