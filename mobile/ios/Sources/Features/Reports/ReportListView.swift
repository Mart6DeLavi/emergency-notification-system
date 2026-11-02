import SwiftUI

struct ReportListView: View {
    @EnvironmentObject private var session: SessionStore
    @StateObject private var viewModel = ReportsViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 12) {
                    if viewModel.isLoading && viewModel.reports.isEmpty {
                        ProgressView()
                            .tint(.white)
                            .padding(.top, 60)
                    } else if viewModel.reports.isEmpty {
                        emptyState
                    } else {
                        if let error = viewModel.errorMessage {
                            ErrorBanner(message: error)
                        }

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
                .padding(16)
            }
            .navigationTitle("My reports")
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

    private var emptyState: some View {
        VStack(spacing: 14) {
            Image(systemName: "shield.checkered")
                .font(.system(size: 52))
                .foregroundStyle(SensaTheme.accent)

            Text("No reports yet")
                .font(.headline)
                .foregroundStyle(.white)

            Text("Create your first emergency report from the New report tab.")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.6))
                .multilineTextAlignment(.center)

            if let error = viewModel.errorMessage {
                ErrorBanner(message: error)
            }

            SensaButton(title: "Retry", style: .secondary) {
                Task { await viewModel.load(session: session) }
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
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
