package com.sensa.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensa.mobile.data.Emergency

val SensaBg = Color(0xFF080A0D)
val SensaSurface = Color(0xFF151A21)
val SensaSurfaceAlt = Color(0xFF101319)
val SensaBorder = Color(0xFF232B36)
val SensaText = Color(0xFFE5E7EB)
val SensaMuted = Color(0xFF94A3B8)
val SensaAccent = Color(0xFF22D3EE)
val SensaSuccess = Color(0xFF34D399)
val SensaWarning = Color(0xFFFBBF24)
val SensaDanger = Color(0xFFF87171)

@Composable
fun SensaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = SensaAccent,
            onPrimary = Color(0xFF041014),
            background = SensaBg,
            onBackground = SensaText,
            surface = SensaSurface,
            onSurface = SensaText,
            surfaceVariant = SensaSurfaceAlt,
            onSurfaceVariant = SensaMuted,
            outline = SensaBorder,
            error = SensaDanger
        ),
        content = content
    )
}

@Composable
fun ScreenBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0E14), SensaBg, Color(0xFF07090C))
                )
            )
    ) {
        content()
    }
}

@Composable
fun SensaCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SensaSurface, RoundedCornerShape(18.dp))
            .border(1.dp, SensaBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SensaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    destructive: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) Color(0xFF7F1D1D) else SensaAccent,
            contentColor = if (destructive) Color.White else Color(0xFF041014),
            disabledContainerColor = SensaBorder,
            disabledContentColor = SensaMuted
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = if (destructive) Color.White else Color(0xFF041014)
            )
        } else {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun SensaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        enabled = enabled,
        minLines = minLines,
        shape = RoundedCornerShape(13.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SensaSurfaceAlt,
            unfocusedContainerColor = SensaSurfaceAlt,
            disabledContainerColor = SensaSurfaceAlt,
            focusedTextColor = SensaText,
            unfocusedTextColor = SensaText,
            focusedLabelColor = SensaAccent,
            unfocusedLabelColor = SensaMuted,
            cursorColor = SensaAccent,
            focusedIndicatorColor = SensaAccent,
            unfocusedIndicatorColor = SensaBorder
        )
    )
}

@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = SensaMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        if (trailing != null) {
            Text(trailing, color = SensaMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val normalized = status.uppercase()
    val (bg, fg) = when (normalized) {
        "REPORTED" -> Color(0xFF1E3A5F) to Color(0xFF93C5FD)
        "UNDER_REVIEW" -> Color(0xFF4A3A12) to SensaWarning
        "CONFIRMED" -> Color(0xFF14532D) to SensaSuccess
        "BROADCAST" -> Color(0xFF3B0764) to Color(0xFFD8B4FE)
        "REJECTED" -> Color(0xFF4C1D1D) to SensaDanger
        else -> Color(0xFF273244) to SensaMuted
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            statusLabel(normalized),
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

fun statusLabel(status: String): String = when (status.uppercase()) {
    "REPORTED" -> "REPORTED"
    "UNDER_REVIEW" -> "UNDER REVIEW"
    "CONFIRMED" -> "CONFIRMED"
    "BROADCAST" -> "BROADCAST"
    "REJECTED" -> "REJECTED"
    else -> status.uppercase()
}

fun locationText(emergency: Emergency): String {
    val parts = listOfNotNull(emergency.street, emergency.city, emergency.country)
        .filter { it.isNotBlank() }
    return if (parts.isEmpty()) "Location not specified" else parts.joinToString(", ")
}

fun shortDate(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return value.replace("T", " ").take(16)
}
