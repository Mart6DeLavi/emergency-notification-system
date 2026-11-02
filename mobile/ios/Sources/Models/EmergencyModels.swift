import Foundation

enum EmergencyStatus: String, Codable, CaseIterable, Identifiable, Hashable {
    case reported = "REPORTED"
    case underReview = "UNDER_REVIEW"
    case confirmed = "CONFIRMED"
    case broadcast = "BROADCAST"
    case rejected = "REJECTED"

    var id: String { rawValue }

    var isFinal: Bool {
        self == .broadcast || self == .rejected
    }
}

struct EmergencyFile: Codable, Hashable, Identifiable {
    let fileId: Int64
    let url: String

    var id: Int64 { fileId }
}

struct Emergency: Decodable, Identifiable {
    let id: Int64
    let userId: UUID
    let title: String
    let description: String?
    let files: [EmergencyFile]?
    let country: String?
    let city: String?
    let street: String?
    let alarmTimestamp: String?
    let status: String
    let createdAt: String?

    var statusKind: EmergencyStatus {
        EmergencyStatus(rawValue: status) ?? .reported
    }

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case title
        case description
        case files
        case country
        case city
        case street
        case alarmTimestamp = "alarm_timestamp"
        case status
        case createdAt = "created_at"
    }
}

struct CreateEmergencyRequest: Encodable {
    let title: String
    let description: String?
    let files: [EmergencyFile]
    let country: String?
    let city: String?
    let street: String?
    let alarmTimestamp: String

    enum CodingKeys: String, CodingKey {
        case title
        case description
        case files
        case country
        case city
        case street
        case alarmTimestamp = "alarm_timestamp"
    }
}
