import Foundation

struct AuthResponse: Decodable {
    let token: String
    let userId: UUID
    let email: String
}

struct LoginRequest: Encodable {
    let email: String
    let password: String
}

struct RegisterRequest: Encodable {
    let email: String
    let password: String
    let firstName: String
    let lastName: String
    let phoneNumber: String
    let country: String
    let city: String
    let street: String
    let homeNumber: String
}
