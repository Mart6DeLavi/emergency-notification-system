package com.sensa.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensa.mobile.data.ApiClient
import com.sensa.mobile.data.LoginRequest
import com.sensa.mobile.data.RegisterRequest
import com.sensa.mobile.data.SessionStore
import com.sensa.mobile.ui.SensaAccent
import com.sensa.mobile.ui.SensaButton
import com.sensa.mobile.ui.SensaDanger
import com.sensa.mobile.ui.SensaMuted
import com.sensa.mobile.ui.SensaTextField
import com.sensa.mobile.ui.ScreenBackground
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    api: ApiClient,
    session: SessionStore,
    onAuthenticated: () -> Unit
) {
    var registering by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Poland") }
    var city by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var homeNumber by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (loading) return
        if (email.isBlank() || password.isBlank()) {
            error = "Enter your email and password."
            return
        }
        if (registering && listOf(firstName, lastName, phoneNumber, country, city, street, homeNumber).any { it.isBlank() }) {
            error = "Fill in all profile and address fields."
            return
        }
        scope.launch {
            loading = true
            error = null
            try {
                val auth = if (registering) {
                    api.service.register(
                        RegisterRequest(
                            email = email.trim(),
                            password = password,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            phoneNumber = phoneNumber.trim(),
                            country = country.trim(),
                            city = city.trim(),
                            street = street.trim(),
                            homeNumber = homeNumber.trim()
                        )
                    )
                } else {
                    api.service.login(LoginRequest(email.trim(), password))
                }
                session.save(auth)
                onAuthenticated()
            } catch (e: Exception) {
                error = api.errorMessage(e)
            } finally {
                loading = false
            }
        }
    }

    ScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.HealthAndSafety,
                    contentDescription = null,
                    tint = SensaAccent
                )
                Spacer(Modifier.padding(4.dp))
                Text(
                    "SENSA",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                "Emergency notification system",
                color = SensaMuted,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))

            Text(
                if (registering) "Create your account" else "Sign in",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (registering) "Your address is used to target nearby alerts."
                else "Access rescue alerts and your reports.",
                color = SensaMuted,
                fontSize = 13.sp
            )

            SensaTextField(email, { email = it }, "Email", singleLine = true)
            SensaTextField(
                password,
                { password = it },
                "Password",
                singleLine = true
            )

            if (registering) {
                Text("Personal information", color = SensaMuted, fontSize = 12.sp)
                SensaTextField(firstName, { firstName = it }, "First name")
                SensaTextField(lastName, { lastName = it }, "Last name")
                SensaTextField(
                    phoneNumber,
                    { phoneNumber = it },
                    "Phone number",
                    singleLine = true
                )
                Text("Address", color = SensaMuted, fontSize = 12.sp)
                SensaTextField(country, { country = it }, "Country")
                SensaTextField(city, { city = it }, "City")
                SensaTextField(street, { street = it }, "Street")
                SensaTextField(homeNumber, { homeNumber = it }, "Home number")
            }

            error?.let {
                Text(it, color = SensaDanger, fontSize = 12.sp)
            }

            Spacer(Modifier.height(4.dp))
            SensaButton(
                text = if (registering) "Create account" else "Sign in",
                onClick = ::submit,
                loading = loading
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        registering = !registering
                        error = null
                    }
                ) {
                    Text(
                        if (registering) "I already have an account" else "Create an account",
                        color = SensaAccent,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
