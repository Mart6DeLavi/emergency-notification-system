import SwiftUI

struct ReportDetailView: View {
    let emergencyId: Int64

    @EnvironmentObject private var session: SessionStore
    @State private var report: Emergency?
    @State private var errorMessage: String?
    @State private var isLoading = false

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
                if let url = URL(string: file.url) {
                    if isImage(url) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                            case .failure:
                                Image(systemName: "photo")
                                    .foregroundStyle(.white.opacity(0.4))
                            default:
                                ProgressView()
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 180)
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    Link(destination: url) {
                        Label("Open attachment", systemImage: "arrow.up.right.square")
                            .font(.subheadline)
                            .foregroundStyle(SensaTheme.accent)
                    }
                }
            }
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
        } catch {
            if !silent {
                errorMessage = error.localizedDescription
            }
        }
    }
}
