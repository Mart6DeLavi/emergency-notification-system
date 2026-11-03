import Foundation
import Combine

@MainActor
final class ProfileViewModel: ObservableObject {
    @Published var firstName = ""
    @Published var lastName = ""
    @Published var email = ""
    @Published var phoneNumber = ""
    @Published var country = ""
    @Published var city = ""
    @Published var street = ""
    @Published var homeNumber = ""

    @Published var pushEnabled = true
    @Published var emailEnabled = true
    @Published var smsEnabled = true

    @Published private(set) var isLoading = false
    @Published private(set) var isSaving = false
    @Published var errorMessage: String?
    @Published var savedMessage: String?

    func load(session: SessionStore) async {
        guard let userId = session.userId else { return }

        isLoading = true
        defer { isLoading = false }

        do {
            let profile = try await session.api.getUser(userId: userId)
            populate(from: profile)
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func save(session: SessionStore) async {
        guard let userId = session.userId else { return }

        isSaving = true
        errorMessage = nil
        savedMessage = nil
        defer { isSaving = false }

        let payload = UserUpdateRequest(
            firstName: firstName.isEmpty ? nil : firstName,
            lastName: lastName.isEmpty ? nil : lastName,
            email: email.isEmpty ? nil : email,
            phoneNumber: phoneNumber.isEmpty ? nil : phoneNumber,
            country: country.isEmpty ? nil : country,
            city: city.isEmpty ? nil : city,
            street: street.isEmpty ? nil : street,
            homeNumber: homeNumber.isEmpty ? nil : homeNumber,
            notificationSettings: NotificationSettings(
                push: pushEnabled,
                emailEnabled: emailEnabled,
                sms: smsEnabled
            ),
            deviceToken: nil
        )

        do {
            let profile = try await session.api.updateUser(userId: userId, payload: payload)
            populate(from: profile)
            savedMessage = "Changes saved."
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func populate(from profile: UserProfile) {
        firstName = profile.firstName
        lastName = profile.lastName
        email = profile.email
        phoneNumber = profile.phoneNumber
        country = profile.country ?? ""
        city = profile.city ?? ""
        street = profile.street ?? ""
        homeNumber = profile.homeNumber ?? ""

        if let settings = profile.notificationSettings {
            pushEnabled = settings.push
            emailEnabled = settings.emailEnabled
            smsEnabled = settings.sms
        }
    }
}
