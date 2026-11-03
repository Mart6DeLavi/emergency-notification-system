import SwiftUI
import PhotosUI

struct NewReportView: View {
    @EnvironmentObject private var session: SessionStore
    @StateObject private var viewModel = NewReportViewModel()
    @State private var pickerItems: [PhotosPickerItem] = []

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    detailsCard
                    attachmentsCard

                    if let error = viewModel.errorMessage {
                        ErrorBanner(message: error)
                    }

                    SensaButton(title: "Submit report", isLoading: viewModel.isSubmitting) {
                        Task { await viewModel.submit(session: session) }
                    }
                    .disabled(!viewModel.canSubmit)
                }
                .padding(16)
            }
            .navigationTitle("New report")
            .sensaScreenBackground()
            .onChange(of: pickerItems) { _, newItems in
                guard !newItems.isEmpty else { return }
                Task {
                    await viewModel.add(items: newItems)
                    pickerItems = []
                }
            }
            .alert(
                "Report submitted",
                isPresented: Binding(
                    get: { viewModel.successMessage != nil },
                    set: { if !$0 { viewModel.successMessage = nil } }
                )
            ) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("Your report was created and the approved files were linked to it.")
            }
        }
    }

    private var detailsCard: some View {
        SensaCard {
            SectionHeader(title: "Details")

            SensaField(title: "Title", text: $viewModel.title, autocapitalization: .sentences)
            SensaTextEditor(title: "Description", text: $viewModel.description)

            SectionHeader(title: "Location")
            SensaField(title: "Country", text: $viewModel.country, autocapitalization: .words)
            SensaField(title: "City", text: $viewModel.city, autocapitalization: .words)
            SensaField(title: "Street", text: $viewModel.street, autocapitalization: .words)
        }
    }

    private var attachmentsCard: some View {
        SensaCard {
            SectionHeader(title: "Attachments")

            Text("Add photos or videos. Each file is checked by moderation before you can submit the report.")
                .font(.footnote)
                .foregroundStyle(.white.opacity(0.6))

            PhotosPicker(
                selection: $pickerItems,
                maxSelectionCount: 10,
                matching: .any(of: [.images, .videos])
            ) {
                Label("Choose photos or videos", systemImage: "photo.on.rectangle.angled")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(SensaTheme.accent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(SensaTheme.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(SensaTheme.border, lineWidth: 1)
                    )
            }

            if viewModel.attachments.isEmpty {
                Text("No files selected.")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.45))
            } else {
                ForEach(viewModel.attachments) { attachment in
                    attachmentRow(attachment)
                }
            }

            if viewModel.hasRejected {
                Text("Rejected files cannot be submitted. Remove them and add a replacement.")
                    .font(.caption)
                    .foregroundStyle(SensaTheme.danger)
            }

            SensaButton(
                title: "Upload & check",
                style: .secondary,
                isLoading: viewModel.isUploading
            ) {
                Task { await viewModel.upload(session: session) }
            }
            .disabled(!viewModel.canUpload)
        }
    }

    private func attachmentRow(_ attachment: PendingAttachment) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "paperclip")
                .foregroundStyle(.white.opacity(0.5))

            VStack(alignment: .leading, spacing: 4) {
                Text(attachment.filename)
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.75))
                    .lineLimit(1)

                if let result = attachment.result {
                    HStack(spacing: 4) {
                        if result.isApproved {
                            Text("Approved")
                        } else {
                            Text("Rejected")
                        }
                        Text("·")
                        Text("Confidence")
                        Text("\(Int(result.confidencePercentage))%")
                    }
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(result.isApproved ? SensaTheme.success : SensaTheme.danger)
                } else {
                    Text("Not checked yet")
                        .font(.caption2)
                        .foregroundStyle(.white.opacity(0.4))
                }
            }

            Spacer()

            Button {
                viewModel.remove(attachment)
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(.white.opacity(0.4))
            }
            .buttonStyle(.plain)
        }
        .padding(10)
        .background(SensaTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}
