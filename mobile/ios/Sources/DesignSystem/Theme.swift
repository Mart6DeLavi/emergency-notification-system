import SwiftUI

enum SensaTheme {
    static let backgroundTop = Color(red: 0.05, green: 0.13, blue: 0.24)
    static let background = Color(red: 0.02, green: 0.05, blue: 0.10)
    static let surface = Color.white.opacity(0.06)
    static let surfaceStrong = Color.white.opacity(0.10)
    static let border = Color.white.opacity(0.12)
    static let accent = Color(red: 0.38, green: 0.68, blue: 1.0)
    static let success = Color(red: 0.30, green: 0.80, blue: 0.55)
    static let warning = Color(red: 1.0, green: 0.72, blue: 0.30)
    static let danger = Color(red: 1.0, green: 0.38, blue: 0.38)
}

extension View {
    func sensaScreenBackground() -> some View {
        background(
            LinearGradient(
                colors: [SensaTheme.backgroundTop, SensaTheme.background],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
        )
    }
}
