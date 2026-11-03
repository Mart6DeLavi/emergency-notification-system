import SwiftUI

struct AlertDetailView: View {
    let alert: SystemAlert

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 8) {
                    Image(systemName: "megaphone.fill")
                        .foregroundStyle(SensaTheme.accent)
                    Text("Rescue service alert")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(SensaTheme.accent)
                    Spacer()
                    Text(DateText.display(alert.createdAt))
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.5))
                }

                Text(alert.title)
                    .font(.title2.weight(.bold))
                    .foregroundStyle(.white)

                Text(alert.content)
                    .font(.body)
                    .foregroundStyle(.white.opacity(0.8))
                    .fixedSize(horizontal: false, vertical: true)

                Divider()
                    .overlay(.white.opacity(0.1))

                HStack(spacing: 12) {
                    Label("Rescue service", systemImage: "shield.lefthalf.filled")
                    if let channel = alert.channel, !channel.isEmpty {
                        Label(channel.capitalized, systemImage: "bell")
                    }
                }
                .font(.caption)
                .foregroundStyle(.white.opacity(0.55))

                Spacer(minLength: 0)
            }
            .padding(16)
        }
        .navigationTitle("Alert")
        .navigationBarTitleDisplayMode(.inline)
        .sensaScreenBackground()
    }
}
