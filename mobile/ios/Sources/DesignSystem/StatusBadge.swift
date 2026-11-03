import SwiftUI

struct StatusBadge: View {
    let status: EmergencyStatus

    var body: some View {
        Text(status.title)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(color.opacity(0.18))
            .foregroundStyle(color)
            .clipShape(Capsule())
    }

    private var color: Color {
        switch status {
        case .reported:
            return SensaTheme.accent
        case .underReview:
            return SensaTheme.warning
        case .confirmed:
            return SensaTheme.success
        case .broadcast:
            return SensaTheme.accent
        case .rejected:
            return SensaTheme.danger
        }
    }
}

extension EmergencyStatus {
    var title: LocalizedStringKey {
        switch self {
        case .reported:
            return "Reported"
        case .underReview:
            return "Under review"
        case .confirmed:
            return "Confirmed"
        case .broadcast:
            return "Broadcast"
        case .rejected:
            return "Rejected"
        }
    }
}
