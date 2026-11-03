import Foundation
import Combine

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var email = ""
    @Published var password = ""

    @Published var firstName = ""
    @Published var lastName = ""
    @Published var phoneNumber = ""
    @Published var country = ""
    @Published var city = ""
    @Published var street = ""
    @Published var homeNumber = ""

    @Published private(set) var isLoading = false
    @Published var errorMessage: String?

    func login(session: SessionStore) async {
        guard let error = validateLogin() else {
            await performLogin(session: session)
            return
        }
        errorMessage = error
    }

    func register(session: SessionStore) async {
        guard let error = validateRegistration() else {
            await performRegister(session: session)
            return
        }
        errorMessage = error
    }

    private func performLogin(session: SessionStore) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            let response = try await session.api.login(
                email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
                password: password
            )
            session.signIn(response)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func performRegister(session: SessionStore) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        let payload = RegisterRequest(
            email: email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            password: password,
            firstName: firstName.trimmingCharacters(in: .whitespacesAndNewlines),
            lastName: lastName.trimmingCharacters(in: .whitespacesAndNewlines),
            phoneNumber: phoneNumber.trimmingCharacters(in: .whitespacesAndNewlines),
            country: country.trimmingCharacters(in: .whitespacesAndNewlines),
            city: city.trimmingCharacters(in: .whitespacesAndNewlines),
            street: street.trimmingCharacters(in: .whitespacesAndNewlines),
            homeNumber: homeNumber.trimmingCharacters(in: .whitespacesAndNewlines)
        )

        do {
            let response = try await session.api.register(payload)
            session.signIn(response)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func validateLogin() -> String? {
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedEmail.isEmpty {
            return "Enter your email address."
        }
        if !Self.isValidEmail(trimmedEmail) {
            return "Enter a valid email address."
        }
        if password.isEmpty {
            return "Enter your password."
        }
        return nil
    }

    private func validateRegistration() -> String? {
        let trimmedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedEmail.isEmpty {
            return "Enter your email address."
        }
        if !Self.isValidEmail(trimmedEmail) {
            return "Enter a valid email address."
        }
        if password.count < 8 {
            return "Password must be at least 8 characters."
        }
        if firstName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            lastName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Enter your first and last name."
        }
        if phoneNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Enter your phone number."
        }
        if country.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            city.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            street.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            homeNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Enter your full address."
        }
        return nil
    }

    static func isValidEmail(_ value: String) -> Bool {
        let pattern = #"^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"#
        return value.range(of: pattern, options: .regularExpression) != nil
    }
}
