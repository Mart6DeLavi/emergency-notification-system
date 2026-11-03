package com.sensa.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sensa.mobile.data.ApiClient
import com.sensa.mobile.data.Emergency
import com.sensa.mobile.data.EmergencyFile
import com.sensa.mobile.data.SystemAlert
import com.sensa.mobile.ui.SensaAccent
import com.sensa.mobile.ui.SensaButton
import com.sensa.mobile.ui.SensaCard
import com.sensa.mobile.ui.SensaDanger
import com.sensa.mobile.ui.SensaMuted
import com.sensa.mobile.ui.SensaTextField
import com.sensa.mobile.ui.ScreenBackground
import com.sensa.mobile.ui.SectionTitle
import com.sensa.mobile.ui.SensaTheme
import com.sensa.mobile.ui.StatusBadge
import com.sensa.mobile.ui.locationText
import com.sensa.mobile.ui.shortDate

@Composable
fun EmergencyDetailScreen(
    api: ApiClient,
    emergencyId: Long,
    onBack: () -> Unit
) {
    var emergency by remember { mutableStateOf<Emergency?>(null) }
    var fileUrls by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(emergencyId) {
        loading = true
        try {
            val value = api.service.getEmergency(emergencyId)
            emergency = value
            value.files?.forEach { file ->
                runCatching { api.service.getFileUrl(file.fileId) }
                    .onSuccess { result -> fileUrls = fileUrls + (file.fileId to result.url) }
            }
        } catch (e: Exception) {
            error = api.errorMessage(e)
        } finally {
            loading = false
        }
    }

    BackHandler(onBack = onBack)

    ScreenBackground {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = SensaMuted)
                }
                Text("Report details", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

        when {
            loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SensaAccent)
            }

            error != null -> Text(error!!, color = SensaDanger, fontSize = 12.sp)

            emergency != null -> {
                val value = emergency!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        value.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(value.status)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = SensaMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(locationText(value), color = SensaMuted, fontSize = 12.sp)
                }
                Text(shortDate(value.createdAt), color = SensaMuted, fontSize = 11.sp)

                if (!value.description.isNullOrBlank()) {
                    SensaCard {
                        SectionTitle("DESCRIPTION")
                        Spacer(Modifier.height(6.dp))
                        Text(value.description, color = Color.White, fontSize = 14.sp)
                    }
                }

                SensaCard {
                    SectionTitle("INCIDENT ADDRESS")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOfNotNull(value.street, value.city, value.country).filter { it.isNotBlank() }
                            .joinToString(", ").ifBlank { "Not specified" },
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                SectionTitle("ATTACHMENTS", "${value.files?.size ?: 0}")
                val files = value.files.orEmpty()
                if (files.isEmpty()) {
                    SensaCard {
                        Text("No attachments.", color = SensaMuted, fontSize = 12.sp)
                    }
                } else {
                    files.forEach { file ->
                        val url = fileUrls[file.fileId]
                        SensaCard {
                            val isImage = isImageAttachment(file.url)
                            if (isImage && url != null) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Report attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .background(Color(0xFF0B111B), RoundedCornerShape(12.dp))
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isImage) Icons.Outlined.Image else Icons.Outlined.PlayCircle,
                                        contentDescription = null,
                                        tint = SensaAccent
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (isImage) "Loading image..." else "Video attachment",
                                        color = SensaMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (url != null) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0E2A33), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Open attachment",
                                        color = SensaAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 2.dp)
                                    )
                                    IconButton(onClick = { uriHandler.openUri(url) }) {
                                        Icon(
                                            Icons.Outlined.Description,
                                            contentDescription = "Open",
                                            tint = SensaMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun AlertDetailScreen(
    alert: SystemAlert,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    ScreenBackground {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = SensaMuted)
                }
                Text("Rescue alert", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Campaign, contentDescription = null, tint = SensaAccent)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(alert.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(shortDate(alert.createdAt), color = SensaMuted, fontSize = 11.sp)
                }
            }
            SensaCard {
                Text(
                    alert.content.replace("<br>", "\n"),
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
            Text(
                "Sent by the rescue service.",
                color = SensaMuted,
                fontSize = 11.sp
            )
        }
    }
}

private fun isImageAttachment(url: String): Boolean {
    val clean = url.substringBefore('?').lowercase()
    return listOf(".jpg", ".jpeg", ".png", ".gif", ".webp").any { clean.endsWith(it) }
}
