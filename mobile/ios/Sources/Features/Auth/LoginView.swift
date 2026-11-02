import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var session: SessionStore
    @StateObject private var viewModel = AuthViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                brand

                SensaCard {
                    SectionHeader(title: "Sign in")
                    Text("Use the email and password you registered with SENSA.")
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.6))

                    SensaField(
                        title: "Email",
                        text: $viewModel.email,
                        keyboardType: .emailAddress,
                        textContentType: .emailAddress
                    )
                    SensaField(
                        title: "Password",
                        text: $viewModel.password,
                        textContentType: .password,
                        isSecure: true
                    )

                    if let error = viewModel.errorMessage {
                        ErrorBanner(message: error)
                    }

                    SensaButton(title: "Sign in", isLoading: viewModel.isLoading) {
                        Task { await viewModel.login(session: session) }
                    }

                    NavigationLink {
                        RegisterView()
                    } label: {
                        Text("Create an account")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(SensaTheme.accent)
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            .padding(20)
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .navigationBar)
        .sensaScreenBackground()
    }

    private var brand: some View {
        VStack(spacing: 12) {
            Image(systemName: "shield.lefthalf.filled")
                .font(.system(size: 64, weight: .semibold))
                .foregroundStyle(.white)

            Text("SENSA")
                .font(.system(size: 38, weight: .bold, design: .rounded))
                .tracking(2)
                .foregroundStyle(.white)

            Text("Emergency Notification System")
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.65))
        }
        .padding(.top, 40)
        .padding(.bottom, 8)
    }
}
