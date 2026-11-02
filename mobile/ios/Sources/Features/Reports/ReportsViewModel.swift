import Foundation
import Combine

@MainActor
final class ReportsViewModel: ObservableObject {
    @Published private(set) var alerts: [SystemAlert] = []
    @Published private(set) var reports: [Emergency] = []
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?

    func load(session: SessionStore) async {
        if isLoading { return }

        isLoading = true
        defer { isLoading = false }

        var errors: [String] = []

        do {
            alerts = try await session.api.getMyAlerts()
        } catch {
            errors.append("Alerts: \(error.localizedDescription)")
        }

        do {
            reports = try await session.api.getMyEmergencies()
        } catch {
            errors.append("Reports: \(error.localizedDescription)")
        }

        errorMessage = errors.first
    }
}
