import SwiftUI

@main
struct SENSAApp: App {
    @StateObject private var session = SessionStore()
    @AppStorage("appLanguage") private var appLanguage = "en"

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(session)
                .environment(\.locale, Locale(identifier: appLanguage))
                .preferredColorScheme(.dark)
        }
    }
}
