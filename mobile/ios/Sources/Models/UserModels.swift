import Foundation

struct NotificationSettings: Codable, Equatable {
    var push: Bool
    var emailEnabled: Bool
    var sms: Bool
}

struct UserProfile: Decodable {
    let userId: UUID
    let firstName: String
    let lastName: String
    let email: String
    let phoneNumber: String
    let country: String?
    let city: String?
    let street: String?
    let homeNumber: String?
    let notificationSettings: NotificationSettings?
    let createdAt: String?
    let updatedAt: String?
}

struct UserUpdateRequest: Encodable {
    var firstName: String?
    var lastName: String?
    var email: String?
    var phoneNumber: String?
    var country: String?
    var city: String?
    var street: String?
    var homeNumber: String?
    var notificationSettings: NotificationSettings?
    var deviceToken: String?
}
