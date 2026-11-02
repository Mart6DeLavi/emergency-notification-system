import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            ReportListView()
                .tabItem {
                    Label("Reports", systemImage: "list.bullet.rectangle")
                }

            NewReportView()
                .tabItem {
                    Label("New report", systemImage: "plus.circle.fill")
                }

            ProfileView()
                .tabItem {
                    Label("Profile", systemImage: "person.crop.circle")
                }
        }
        .tint(SensaTheme.accent)
    }
}
