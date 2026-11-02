import Foundation
import Combine

@MainActor
final class ReportsViewModel: ObservableObject {
    @Published private(set) var reports: [Emergency] = []
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?

    func load(session: SessionStore) async {
        if isLoading { return }

        isLoading = true
        defer { isLoading = false }

        do {
            reports = try await session.api.getMyEmergencies()
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
