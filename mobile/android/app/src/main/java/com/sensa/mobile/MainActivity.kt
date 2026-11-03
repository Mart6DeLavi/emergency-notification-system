package com.sensa.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sensa.mobile.data.ApiClient
import com.sensa.mobile.data.Emergency
import com.sensa.mobile.data.SessionStore
import com.sensa.mobile.data.SystemAlert
import com.sensa.mobile.ui.SensaAccent
import com.sensa.mobile.ui.SensaBg
import com.sensa.mobile.ui.SensaMuted
import com.sensa.mobile.ui.SensaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensaTheme {
                SensaApp()
            }
        }
    }
}

@Composable
private fun SensaApp() {
    val context = LocalContext.current
    val session = remember { SessionStore(context) }
    val api = remember { ApiClient(session) }
    var authenticated by remember { mutableStateOf(session.isLoggedIn) }

    if (authenticated) {
        MainScreen(
            api = api,
            session = session,
            onLoggedOut = {
                session.clear()
                authenticated = false
            }
        )
    } else {
        AuthScreen(
            api = api,
            session = session,
            onAuthenticated = {
                authenticated = true
            }
        )
    }
}

@Composable
private fun MainScreen(
    api: ApiClient,
    session: SessionStore,
    onLoggedOut: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var selectedEmergency by remember { mutableStateOf<Emergency?>(null) }
    var selectedAlert by remember { mutableStateOf<SystemAlert?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    when {
        selectedAlert != null -> AlertDetailScreen(
            alert = selectedAlert!!,
            onBack = { selectedAlert = null }
        )

        selectedEmergency != null -> EmergencyDetailScreen(
            api = api,
            emergencyId = selectedEmergency!!.id,
            onBack = { selectedEmergency = null }
        )

        else -> Scaffold(
            containerColor = SensaBg,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0D1117)) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Outlined.NotificationsNone, contentDescription = null) },
                        label = { Text("Reports") },
                        colors = navigationColors()
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Outlined.AddCircleOutline, contentDescription = null) },
                        label = { Text("New report") },
                        colors = navigationColors()
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
                        label = { Text("Profile") },
                        colors = navigationColors()
                    )
                }
            }
        ) { padding ->
            when (tab) {
                0 -> ReportsScreen(
                    api = api,
                    refreshKey = refreshKey,
                    onOpenEmergency = { selectedEmergency = it },
                    onOpenAlert = { selectedAlert = it },
                    modifier = Modifier.padding(padding)
                )

                1 -> NewReportScreen(
                    api = api,
                    session = session,
                    onCreated = {
                        refreshKey++
                        tab = 0
                    },
                    modifier = Modifier.padding(padding)
                )

                else -> ProfileScreen(
                    api = api,
                    session = session,
                    onLoggedOut = onLoggedOut,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun navigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = SensaAccent,
    selectedTextColor = SensaAccent,
    indicatorColor = Color(0xFF0E2A33),
    unselectedIconColor = SensaMuted,
    unselectedTextColor = SensaMuted
)
