import SwiftUI

struct RegisterView: View {
    @EnvironmentObject private var session: SessionStore
    @StateObject private var viewModel = AuthViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                SensaCard {
                    SectionHeader(title: "Account")
                    SensaField(
                        title: "Email",
                        text: $viewModel.email,
                        keyboardType: .emailAddress,
                        textContentType: .emailAddress
                    )
                    SensaField(
                        title: "Password",
                        text: $viewModel.password,
                        textContentType: .newPassword,
                        isSecure: true
                    )
                    Text("At least 8 characters.")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.45))
                }

                SensaCard {
                    SectionHeader(title: "Personal information")
                    SensaField(
                        title: "First name",
                        text: $viewModel.firstName,
                        textContentType: .givenName,
                        autocapitalization: .words
                    )
                    SensaField(
                        title: "Last name",
                        text: $viewModel.lastName,
                        textContentType: .familyName,
                        autocapitalization: .words
                    )
                    SensaField(
                        title: "Phone number",
                        text: $viewModel.phoneNumber,
                        keyboardType: .phonePad,
                        textContentType: .telephoneNumber
                    )
                }

                SensaCard {
                    SectionHeader(title: "Address")
                    SensaField(
                        title: "Country",
                        text: $viewModel.country,
                        textContentType: .countryName,
                        autocapitalization: .words
                    )
                    SensaField(
                        title: "City",
                        text: $viewModel.city,
                        textContentType: .addressCity,
                        autocapitalization: .words
                    )
                    SensaField(
                        title: "Street",
                        text: $viewModel.street,
                        textContentType: .streetAddressLine1,
                        autocapitalization: .words
                    )
                    SensaField(
                        title: "Home number",
                        text: $viewModel.homeNumber
                    )
                }

                if let error = viewModel.errorMessage {
                    ErrorBanner(message: error)
                }

                SensaButton(title: "Create account", isLoading: viewModel.isLoading) {
                    Task { await viewModel.register(session: session) }
                }
            }
            .padding(20)
        }
        .navigationTitle("Create account")
        .navigationBarTitleDisplayMode(.inline)
        .sensaScreenBackground()
    }
}
