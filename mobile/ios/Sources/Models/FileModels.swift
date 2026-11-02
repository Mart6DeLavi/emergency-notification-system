import Foundation

struct FileUploadResult: Decodable, Identifiable {
    let fileId: Int64
    let url: String?
    let moderationStatus: String
    let confidencePercentage: Double

    var id: Int64 { fileId }

    var isApproved: Bool {
        moderationStatus.uppercased() == "APPROVED"
    }
}

struct FileLinkRequest: Encodable {
    let emergencySituationId: Int64
    let fileIds: [Int64]
}

struct FileResponse: Decodable, Identifiable {
    let id: Int64
    let userId: UUID?
    let url: String?
    let filename: String?
    let emergencySituationId: Int64?
    let moderationStatus: String?
    let createdAt: String?
}
