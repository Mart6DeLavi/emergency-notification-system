package com.sensa.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensa.mobile.data.ApiClient
import com.sensa.mobile.data.NotificationSettings
import com.sensa.mobile.data.SessionStore
import com.sensa.mobile.data.UserUpdateRequest
import com.sensa.mobile.ui.SensaAccent
import com.sensa.mobile.ui.SensaButton
import com.sensa.mobile.ui.SensaCard
import com.sensa.mobile.ui.SensaDanger
import com.sensa.mobile.ui.SensaMuted
import com.sensa.mobile.ui.SensaTextField
import com.sensa.mobile.ui.SectionTitle
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    api: ApiClient,
    session: SessionStore,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(session.email ?: "") }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var homeNumber by remember { mutableStateOf("") }
    var pushEnabled by remember { mutableStateOf(false) }
    var emailEnabled by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val userId = session.userId ?: return@LaunchedEffect
        loading = true
        try {
            val profile = api.service.getUser(userId)
            firstName = profile.firstName
            lastName = profile.lastName
            email = profile.email
            phone = profile.phoneNumber
            country = profile.country.orEmpty()
            city = profile.city.orEmpty()
            street = profile.street.orEmpty()
            homeNumber = profile.homeNumber.orEmpty()
            pushEnabled = profile.notificationSettings?.push ?: false
            emailEnabled = profile.notificationSettings?.emailEnabled ?: true
        } catch (e: Exception) {
            error = api.errorMessage(e)
        } finally {
            loading = false
        }
    }

    fun save() {
        val userId = session.userId ?: return
        if (saving) return
        saving = true
        saved = false
        error = null
        scope.launch {
            try {
                api.service.updateUser(
                    userId,
                    UserUpdateRequest(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        email = email.trim(),
                        phoneNumber = phone.trim(),
                        country = country.trim(),
                        city = city.trim(),
                        street = street.trim(),
                        homeNumber = homeNumber.trim(),
                        notificationSettings = NotificationSettings(
                            push = pushEnabled,
                            emailEnabled = emailEnabled,
                            sms = false
                        ),
                        deviceToken = null
                    )
                )
                saved = true
            } catch (e: Exception) {
                error = api.errorMessage(e)
            } finally {
                saving = false
            }
        }
    }

    Column(
        modifier = modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("ACCOUNT", color = SensaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Text("Profile", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(email, color = SensaMuted, fontSize = 12.sp)

        SectionTitle("PERSONAL INFORMATION")
        SensaTextField(firstName, { firstName = it }, "First name")
        SensaTextField(lastName, { lastName = it }, "Last name")
        SensaTextField(email, { email = it }, "Email")
        SensaTextField(phone, { phone = it }, "Phone number")

        SectionTitle("ADDRESS")
        SensaTextField(country, { country = it }, "Country")
        SensaTextField(city, { city = it }, "City")
        SensaTextField(street, { street = it }, "Street")
        SensaTextField(homeNumber, { homeNumber = it }, "Home number")

        SectionTitle("NOTIFICATIONS")
        SensaCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ToggleRow("Email alerts", "Receive alerts by email", emailEnabled) { emailEnabled = it }
                ToggleRow("Push alerts", "Reserved for mobile push", pushEnabled) { pushEnabled = it }
            }
        }

        error?.let { Text(it, color = SensaDanger, fontSize = 12.sp) }
        if (saved) {
            Text("Changes saved.", color = Color(0xFF34D399), fontSize = 12.sp)
        }

        SensaButton(
            text = if (saving) "Saving..." else "Save changes",
            onClick = ::save,
            loading = saving,
            enabled = !loading
        )
        Spacer(Modifier.height(4.dp))
        SensaButton(
            text = "Sign out",
            onClick = {
                session.clear()
                onLoggedOut()
            },
            destructive = true
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SensaMuted, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF041014),
                checkedTrackColor = SensaAccent,
                uncheckedThumbColor = SensaMuted,
                uncheckedTrackColor = Color(0xFF273244)
            )
        )
    }
}
