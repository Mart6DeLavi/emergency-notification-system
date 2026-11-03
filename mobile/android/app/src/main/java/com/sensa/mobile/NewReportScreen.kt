package com.sensa.mobile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sensa.mobile.data.ApiClient
import com.sensa.mobile.data.CreateEmergencyRequest
import com.sensa.mobile.data.EmergencyFile
import com.sensa.mobile.data.FileLinkRequest
import com.sensa.mobile.data.FileUploadResult
import com.sensa.mobile.data.SessionStore
import com.sensa.mobile.ui.SensaAccent
import com.sensa.mobile.ui.SensaButton
import com.sensa.mobile.ui.SensaCard
import com.sensa.mobile.ui.SensaDanger
import com.sensa.mobile.ui.SensaMuted
import com.sensa.mobile.ui.SensaSuccess
import com.sensa.mobile.ui.SensaTextField
import com.sensa.mobile.ui.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant

data class MediaItem(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val result: FileUploadResult? = null
)

@Composable
fun NewReportScreen(
    api: ApiClient,
    session: SessionStore,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Poland") }
    var city by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var media by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var uploading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val userId = session.userId ?: return@LaunchedEffect
        runCatching { api.service.getUser(userId) }.onSuccess { profile ->
            country = profile.country ?: country
            city = profile.city ?: city
            street = profile.street ?: street
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val added = uris.map { uri ->
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val name = queryDisplayName(context, uri)
                ?: "upload-${System.currentTimeMillis()}.${extensionFor(mime)}"
            MediaItem(uri, name, mime)
        }
        media = (media + added).take(10)
        error = null
    }

    fun startUpload() {
        if (media.isEmpty() || uploading) return
        uploading = true
        error = null
        scope.launch {
            try {
                val parts = withContext(Dispatchers.IO) {
                    media.map { item ->
                        val bytes = context.contentResolver.openInputStream(item.uri)?.use { it.readBytes() }
                            ?: throw IllegalStateException("Could not read ${item.name}")
                        val body = bytes.toRequestBody(item.mimeType.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("files", item.name, body)
                    }
                }
                val results = api.service.uploadFiles(parts)
                media = media.mapIndexed { index, item ->
                    item.copy(result = results.getOrNull(index))
                }
                if (results.size != media.size) {
                    error = "The server returned an unexpected number of moderation results."
                } else if (results.all { it.isApproved }) {
                    error = null
                }
            } catch (e: Exception) {
                error = api.errorMessage(e)
            } finally {
                uploading = false
            }
        }
    }

    fun submit() {
        if (submitting) return
        if (title.isBlank()) {
            error = "Enter a title for the report."
            return
        }
        if (listOf(country, city, street).any { it.isBlank() }) {
            error = "Enter the country, city and street."
            return
        }
        val approved = media.mapNotNull { it.result }.filter { it.isApproved }
        if (approved.isEmpty()) {
            error = "Upload at least one file that passes moderation."
            return
        }
        val files = approved.mapNotNull { result ->
            result.url?.let { EmergencyFile(result.fileId, it) }
        }
        if (files.size != approved.size) {
            error = "One or more approved files have no URL. Upload them again."
            return
        }

        submitting = true
        error = null
        scope.launch {
            try {
                val created = api.service.createEmergency(
                    CreateEmergencyRequest(
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        files = files,
                        country = country.trim(),
                        city = city.trim(),
                        street = street.trim(),
                        alarmTimestamp = Instant.now().toString()
                    )
                )
                runCatching {
                    api.service.linkFiles(
                        FileLinkRequest(created.id, files.map { it.fileId })
                    )
                }
                media = emptyList()
                title = ""
                description = ""
                onCreated()
            } catch (e: Exception) {
                error = api.errorMessage(e)
            } finally {
                submitting = false
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
        Text("OPERATIONS", color = SensaMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Text("New report", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Describe the incident and attach photos or videos. Files are checked before the report is created.",
            color = SensaMuted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(4.dp))
        SensaTextField(title, { title = it }, "Title")
        SensaTextField(
            description,
            { description = it },
            "Description",
            singleLine = false,
            minLines = 4
        )

        SectionTitle("INCIDENT ADDRESS")
        SensaTextField(country, { country = it }, "Country")
        SensaTextField(city, { city = it }, "City")
        SensaTextField(street, { street = it }, "Street")

        SectionTitle("ATTACHMENTS", "${media.size}/10")
        if (media.isEmpty()) {
            SensaCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = SensaMuted)
                    Spacer(Modifier.width(8.dp))
                    Text("No photos or videos selected.", color = SensaMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(media) { item ->
                    MediaPreview(
                        item = item,
                        onRemove = {
                            media = media.filterNot { it.uri == item.uri }
                            error = null
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SensaButton(
                    text = "Add media",
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    enabled = media.size < 10 && !uploading && !submitting
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SensaButton(
                    text = if (uploading) "Checking..." else "Upload & check",
                    onClick = ::startUpload,
                    enabled = media.isNotEmpty() && !uploading && !submitting,
                    loading = uploading
                )
            }
        }

        if (media.isNotEmpty()) {
            UploadSummary(media)
        }

        error?.let {
            Text(it, color = SensaDanger, fontSize = 12.sp)
        }

        Spacer(Modifier.height(2.dp))
        SensaButton(
            text = if (submitting) "Submitting..." else "Submit report",
            onClick = ::submit,
            enabled = media.any { it.result?.isApproved == true } && !uploading && !submitting,
            loading = submitting
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MediaPreview(item: MediaItem, onRemove: () -> Unit) {
    Column(modifier = Modifier.width(108.dp)) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF111827))
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.name,
            color = SensaMuted,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        item.result?.let { result ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (result.isApproved) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                    contentDescription = null,
                    tint = if (result.isApproved) SensaSuccess else SensaDanger,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    "${result.moderationStatus} · ${"%.1f".format(result.confidencePercentage)}%",
                    color = if (result.isApproved) SensaSuccess else SensaDanger,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun UploadSummary(media: List<MediaItem>) {
    val checked = media.count { it.result != null }
    val approved = media.count { it.result?.isApproved == true }
    val rejected = media.count { it.result?.isApproved == false }
    SensaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = SensaAccent)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "$approved approved · $rejected rejected · ${media.size - checked} not checked",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Rejected files are not attached to the report.",
                    color = SensaMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else {
            null
        }
    }
}

private fun extensionFor(mimeType: String): String = when (mimeType.lowercase()) {
    "image/jpeg" -> "jpg"
    "image/png" -> "png"
    "image/gif" -> "gif"
    "image/webp" -> "webp"
    "video/mp4" -> "mp4"
    "video/webm" -> "webm"
    "video/quicktime" -> "mov"
    else -> "bin"
}
