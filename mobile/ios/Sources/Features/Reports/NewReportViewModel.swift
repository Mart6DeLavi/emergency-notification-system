import Foundation
import Combine
import SwiftUI
import PhotosUI
import UniformTypeIdentifiers

struct PendingAttachment: Identifiable {
    let id = UUID()
    let filename: String
    let mimeType: String
    let data: Data
    var result: FileUploadResult?
}

@MainActor
final class NewReportViewModel: ObservableObject {
    @Published var title = ""
    @Published var description = ""
    @Published var country = ""
    @Published var city = ""
    @Published var street = ""

    @Published private(set) var attachments: [PendingAttachment] = []
    @Published private(set) var isUploading = false
    @Published private(set) var isSubmitting = false
    @Published var errorMessage: String?
    @Published var successMessage: String?

    var approvedResults: [FileUploadResult] {
        attachments.compactMap { $0.result }.filter { $0.isApproved }
    }

    var hasRejected: Bool {
        attachments.contains { $0.result?.isApproved == false }
    }

    var canUpload: Bool {
        !attachments.isEmpty && !isUploading && !isSubmitting
    }

    var canSubmit: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
            !approvedResults.isEmpty &&
            !isUploading &&
            !isSubmitting
    }

    func add(items: [PhotosPickerItem]) async {
        for item in items {
            guard let data = try? await item.loadTransferable(type: Data.self), !data.isEmpty else {
                continue
            }

            let type = item.supportedContentTypes.first
            let mimeType = type?.preferredMIMEType ?? "application/octet-stream"
            let extensionName = type?.preferredFilenameExtension ?? "bin"
            let filename = "attachment-\(UUID().uuidString.prefix(8)).\(extensionName)"

            attachments.append(
                PendingAttachment(filename: filename, mimeType: mimeType, data: data)
            )
        }
    }

    func remove(_ attachment: PendingAttachment) {
        attachments.removeAll { $0.id == attachment.id }
    }

    func upload(session: SessionStore) async {
        guard !attachments.isEmpty else {
            errorMessage = "Select at least one photo or video."
            return
        }

        isUploading = true
        errorMessage = nil
        defer { isUploading = false }

        let files = attachments.map {
            UploadFile(data: $0.data, filename: $0.filename, mimeType: $0.mimeType)
        }

        do {
            let results = try await session.api.uploadFiles(files)

            for index in attachments.indices {
                attachments[index].result = index < results.count ? results[index] : nil
            }

            if results.count != attachments.count {
                errorMessage = "The server returned an unexpected number of moderation results."
            } else if results.allSatisfy({ $0.isApproved }) {
                errorMessage = nil
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func submit(session: SessionStore) async {
        guard let validationError = validate() else {
            await performSubmit(session: session)
            return
        }
        errorMessage = validationError
    }

    func reset() {
        title = ""
        description = ""
        country = ""
        city = ""
        street = ""
        attachments = []
        errorMessage = nil
    }

    private func performSubmit(session: SessionStore) async {
        let approved = approvedResults
        let fileRefs = approved.compactMap { result -> EmergencyFile? in
            guard let url = result.url else { return nil }
            return EmergencyFile(fileId: result.fileId, url: url)
        }

        guard fileRefs.count == approved.count else {
            errorMessage = "One or more approved files have no URL. Please upload them again."
            return
        }

        isSubmitting = true
        errorMessage = nil
        defer { isSubmitting = false }

        let trimmedDescription = description.trimmingCharacters(in: .whitespacesAndNewlines)

        let payload = CreateEmergencyRequest(
            title: title.trimmingCharacters(in: .whitespacesAndNewlines),
            description: trimmedDescription.isEmpty ? nil : trimmedDescription,
            files: fileRefs,
            country: country.trimmingCharacters(in: .whitespacesAndNewlines),
            city: city.trimmingCharacters(in: .whitespacesAndNewlines),
            street: street.trimmingCharacters(in: .whitespacesAndNewlines),
            alarmTimestamp: DateText.nowISO8601()
        )

        do {
            let created = try await session.api.createEmergency(payload)

            _ = try? await session.api.linkFiles(
                FileLinkRequest(
                    emergencySituationId: created.id,
                    fileIds: fileRefs.map(\.fileId)
                )
            )

            successMessage = "Report submitted."
            reset()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func validate() -> String? {
        if title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Enter a title for the report."
        }
        if country.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            city.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
            street.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return "Enter the country, city and street of the incident."
        }
        if attachments.isEmpty {
            return "Select at least one photo or video."
        }
        if approvedResults.isEmpty {
            return "Upload your files first. At least one file must be approved by moderation."
        }
        return nil
    }
}
