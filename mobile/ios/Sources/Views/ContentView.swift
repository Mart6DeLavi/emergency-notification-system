import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.05, green: 0.12, blue: 0.22),
                    Color(red: 0.02, green: 0.05, blue: 0.10)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "shield.lefthalf.filled")
                    .font(.system(size: 92, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.95))

                Text("SENSA")
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .tracking(2)
                    .foregroundStyle(.white)

                Text("Emergency Notification System")
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.white.opacity(0.75))

                Text("Smoke test build")
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.45))
                    .padding(.top, 8)
            }
            .padding(32)
        }
    }
}

#Preview {
    ContentView()
}
