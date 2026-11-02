import SwiftUI

struct ProfileView: View {
    @EnvironmentObject private var session: SessionStore
    @StateObject private var viewModel = ProfileViewModel()
    @AppStorage("appLanguage") private var appLanguage = "en"

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if viewModel.isLoading && viewModel.firstName.isEmpty {
                        ProgressView()
                            .tint(.white)
                            .padding(.top, 60)
                    } else {
                        accountCard
                        addressCard
                        notificationsCard
                        languageCard

                        if let error = viewModel.errorMessage {
                            ErrorBanner(message: error)
                        }

                        if let saved = viewModel.savedMessage {
                            Text(saved)
                                .font(.footnote)
                                .foregroundStyle(SensaTheme.success)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        SensaButton(title: "Save changes", isLoading: viewModel.isSaving) {
                            Task { await viewModel.save(session: session) }
                        }

                        SensaButton(title: "Sign out", style: .destructive) {
                            session.signOut()
                        }
                    }
                }
                .padding(16)
            }
            .navigationTitle("Profile")
            .task {
                await viewModel.load(session: session)
            }
            .sensaScreenBackground()
        }
    }

    private var accountCard: some View {
        SensaCard {
            SectionHeader(title: "Personal information")

            SensaField(
                title: "Email",
                text: $viewModel.email,
                keyboardType: .emailAddress,
                textContentType: .emailAddress
            )
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
    }

    private var addressCard: some View {
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
            SensaField(title: "Home number", text: $viewModel.homeNumber)
        }
    }

    private var notificationsCard: some View {
        SensaCard {
            SectionHeader(title: "Notification settings")

            Toggle("Push notifications", isOn: $viewModel.pushEnabled)
                .tint(SensaTheme.accent)
            Toggle("Email notifications", isOn: $viewModel.emailEnabled)
                .tint(SensaTheme.accent)
            Toggle("SMS notifications", isOn: $viewModel.smsEnabled)
                .tint(SensaTheme.accent)
        }
    }

    private var languageCard: some View {
        SensaCard {
            SectionHeader(title: "Language")

            Picker("Language", selection: $appLanguage) {
                Text("English").tag("en")
                Text("Polski").tag("pl")
            }
            .pickerStyle(.segmented)
        }
    }
}
