import SwiftUI

struct ReportListView: View {
    @EnvironmentObject private var session: SessionStore
    @StateObject private var viewModel = ReportsViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    if viewModel.isLoading && viewModel.alerts.isEmpty && viewModel.reports.isEmpty {
                        ProgressView()
                            .tint(.white)
                            .padding(.top, 60)
                    } else {
                        if let error = viewModel.errorMessage {
                            ErrorBanner(message: error)
                        }

                        alertsSection
                        reportsSection
                    }
                }
                .padding(16)
            }
            .navigationTitle("Events")
            .refreshable {
                await viewModel.load(session: session)
            }
            .task {
                await viewModel.load(session: session)
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await viewModel.load(session: session) }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .tint(.white)
                }
            }
            .sensaScreenBackground()
        }
    }

    private var alertsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(
                title: "Rescue service alerts",
                icon: "megaphone.fill",
                count: viewModel.alerts.count
            )

            if viewModel.alerts.isEmpty {
                emptyCard("No alerts from the rescue service yet.")
            } else {
                ForEach(viewModel.alerts) { alert in
                    NavigationLink {
                        AlertDetailView(alert: alert)
                    } label: {
                        AlertRow(alert: alert)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var reportsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionHeader(
                title: "My reports",
                icon: "doc.text.fill",
                count: viewModel.reports.count
            )

            if viewModel.reports.isEmpty {
                emptyCard("You have not submitted any reports yet.")
            } else {
                ForEach(viewModel.reports) { report in
                    NavigationLink {
                        ReportDetailView(emergencyId: report.id)
                    } label: {
                        ReportRow(report: report)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func sectionHeader(title: String, icon: String, count: Int) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(SensaTheme.accent)
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
            Spacer()
            Text("\(count)")
                .font(.caption.weight(.medium))
                .foregroundStyle(.white.opacity(0.45))
        }
        .padding(.horizontal, 2)
    }

    private func emptyCard(_ message: String) -> some View {
        Text(message)
            .font(.caption)
            .foregroundStyle(.white.opacity(0.45))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color.white.opacity(0.03))
            )
    }
}

private struct AlertRow: View {
    let alert: SystemAlert

    var body: some View {
        SensaCard {
            HStack {
                Image(systemName: "megaphone.fill")
                    .foregroundStyle(SensaTheme.accent)
                Text(alert.title)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.leading)
                Spacer()
                Text(DateText.display(alert.createdAt))
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.45))
            }

            Text(alert.content)
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.65))
                .lineLimit(2)
                .multilineTextAlignment(.leading)
        }
    }
}

private struct ReportRow: View {
    let report: Emergency

    var body: some View {
        SensaCard {
            HStack {
                StatusBadge(status: report.statusKind)
                Spacer()
                Text(DateText.display(report.createdAt))
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.5))
            }

            Text(report.title)
                .font(.headline)
                .foregroundStyle(.white)
                .multilineTextAlignment(.leading)

            if !location.isEmpty {
                Label(location, systemImage: "mappin.and.ellipse")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.55))
            }
        }
    }

    private var location: String {
        [report.street, report.city, report.country]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }
}
