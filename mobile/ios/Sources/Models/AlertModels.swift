import Foundation

/// A system / operator alert delivered by the rescue service.
struct SystemAlert: Decodable, Identifiable {
    let id: String
    let userId: UUID?
    let templateName: String?
    let title: String
    let content: String
    let channel: String?
    let status: String?
    let createdAt: String?
    let updatedAt: String?
}
