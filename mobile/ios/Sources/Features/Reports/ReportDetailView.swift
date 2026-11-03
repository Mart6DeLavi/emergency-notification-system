import AVKit
import SwiftUI

struct ReportDetailView: View {
    let emergencyId: Int64

    @EnvironmentObject private var session: SessionStore
    @State private var report: Emergency?
    @State private var errorMessage: String?
    @State private var isLoading = false
    @State private var fileURLs: [Int64: URL] = [:]
    @State private var previewItem: AttachmentPreviewItem?

    private let flow: [EmergencyStatus] = [.reported, .underReview, .confirmed, .broadcast]

    var body: some View {
        ScrollView {
            if let report {
                VStack(alignment: .leading, spacing: 16) {
                    header(report)

                    SensaCard {
                        Text(report.title)
                            .font(.title3.weight(.bold))
                            .foregroundStyle(.white)

                        Text(report.description?.isEmpty == false ? report.description! : "No description")
                            .font(.subheadline)
                            .foregroundStyle(.white.opacity(0.75))

                        if !location(report).isEmpty {
                            InfoRow(label: "Location", value: location(report))
                        }
                        InfoRow(label: "Reported at", value: DateText.display(report.createdAt))
                    }

                    timeline(report)

                    if let files = report.files, !files.isEmpty {
                        attachments(files)
                    }

                    if let errorMessage {
                        ErrorBanner(message: errorMessage)
                    }
                }
                .padding(16)
            } else if isLoading {
                ProgressView()
                    .tint(.white)
                    .padding(.top, 60)
            } else {
                VStack(spacing: 12) {
                    if let errorMessage {
                        ErrorBanner(message: errorMessage)
                    }
                    SensaButton(title: "Retry", style: .secondary) {
                        Task { await load() }
                    }
                }
                .padding(16)
            }
        }
        .navigationTitle("Report")
        .navigationBarTitleDisplayMode(.inline)
        .refreshable { await load() }
        .task {
            await load()
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 20_000_000_000)
                await load(silent: true)
            }
        }
        .sensaScreenBackground()
        .fullScreenCover(item: $previewItem) { item in
            AttachmentPreviewView(item: item)
        }
    }

    private func header(_ report: Emergency) -> some View {
        HStack {
            StatusBadge(status: report.statusKind)
            Spacer()
            Text("#\(report.id)")
                .font(.caption.monospaced())
                .foregroundStyle(.white.opacity(0.4))
        }
    }

    private func timeline(_ report: Emergency) -> some View {
        SensaCard {
            SectionHeader(title: "Status")

            if report.statusKind == .rejected {
                Label("This report was rejected by the rescue service.", systemImage: "xmark.octagon")
                    .font(.subheadline)
                    .foregroundStyle(SensaTheme.danger)
            } else {
                let currentIndex = flow.firstIndex(of: report.statusKind) ?? 0

                ForEach(Array(flow.enumerated()), id: \.element) { index, status in
                    HStack(spacing: 10) {
                        Image(systemName: index <= currentIndex ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(index <= currentIndex ? SensaTheme.success : .white.opacity(0.25))

                        Text(status.title)
                            .font(.subheadline.weight(index == currentIndex ? .semibold : .regular))
                            .foregroundStyle(index <= currentIndex ? .white : .white.opacity(0.45))
                    }
                }
            }
        }
    }

    private func attachments(_ files: [EmergencyFile]) -> some View {
        SensaCard {
            SectionHeader(title: "Attachments")

            ForEach(files) { file in
                attachmentRow(file)
            }
        }
    }

    @ViewBuilder
    private func attachmentRow(_ file: EmergencyFile) -> some View {
        if let url = fileURLs[file.fileId] {
            Button {
                previewItem = AttachmentPreviewItem(
                    id: file.fileId,
                    url: url,
                    isVideo: !isImage(url)
                )
            } label: {
                Group {
                    if isImage(url) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                            case .failure:
                                placeholderAttachment
                            default:
                                ProgressView()
                                    .tint(.white)
                            }
                        }
                    } else {
                        ZStack {
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.white.opacity(0.06))
                            VStack(spacing: 8) {
                                Image(systemName: "play.circle.fill")
                                    .font(.system(size: 42))
                                    .foregroundStyle(.white.opacity(0.85))
                                Text("Play video")
                                    .font(.caption)
                                    .foregroundStyle(.white.opacity(0.6))
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 180)
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)

            Text("Attachment #\(file.fileId)")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.4))
        } else {
            HStack(spacing: 8) {
                ProgressView()
                    .tint(.white)
                Text("Loading attachment…")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.5))
            }
        }
    }

    private var placeholderAttachment: some View {
        ZStack {
            Color.white.opacity(0.06)
            Image(systemName: "photo")
                .foregroundStyle(.white.opacity(0.4))
        }
    }

    private func location(_ report: Emergency) -> String {
        [report.street, report.city, report.country]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }

    private func isImage(_ url: URL) -> Bool {
        ["jpg", "jpeg", "png", "gif", "webp"].contains(url.pathExtension.lowercased())
    }

    private func loadFileURLs() async {
        guard let files = report?.files, !files.isEmpty else { return }

        for file in files where fileURLs[file.fileId] == nil {
            do {
                let response = try await session.api.getFileURL(fileId: file.fileId)
                if let url = URL(string: response.url) {
                    fileURLs[file.fileId] = url
                }
            } catch {
                // Keep the placeholder; the next poll will retry.
            }
        }
    }

    private func load(silent: Bool = false) async {
        if !silent {
            isLoading = true
        }
        defer {
            if !silent {
                isLoading = false
            }
        }

        do {
            report = try await session.api.getEmergency(id: emergencyId)
            errorMessage = nil
            await loadFileURLs()
        } catch {
            if !silent {
                errorMessage = error.localizedDescription
            }
        }
    }
}

private struct AttachmentPreviewItem: Identifiable {
    let id: Int64
    let url: URL
    let isVideo: Bool
}

private struct AttachmentPreviewView: View {
    let item: AttachmentPreviewItem

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()

            if item.isVideo {
                VideoPlayer(player: AVPlayer(url: item.url))
                    .ignoresSafeArea()
            } else {
                AsyncImage(url: item.url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                    case .failure:
                        Image(systemName: "photo")
                            .font(.largeTitle)
                            .foregroundStyle(.white.opacity(0.5))
                    default:
                        ProgressView()
                            .tint(.white)
                    }
                }
                .ignoresSafeArea()
            }

            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .font(.title)
                    .foregroundStyle(.white.opacity(0.85))
                    .padding()
            }
        }
    }
}
