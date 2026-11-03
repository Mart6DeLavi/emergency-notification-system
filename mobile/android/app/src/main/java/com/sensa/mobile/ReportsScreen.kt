package com.sensa.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sensa.mobile.data.ApiClient
import com.sensa.mobile.data.Emergency
import com.sensa.mobile.data.SystemAlert
import com.sensa.mobile.ui.SensaAccent
import com.sensa.mobile.ui.SensaCard
import com.sensa.mobile.ui.SensaDanger
import com.sensa.mobile.ui.SensaMuted
import com.sensa.mobile.ui.SectionTitle
import com.sensa.mobile.ui.StatusBadge
import com.sensa.mobile.ui.locationText
import com.sensa.mobile.ui.shortDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun ReportsScreen(
    api: ApiClient,
    refreshKey: Int,
    onOpenEmergency: (Emergency) -> Unit,
    onOpenAlert: (SystemAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    var alerts by remember { mutableStateOf<List<SystemAlert>>(emptyList()) }
    var reports by remember { mutableStateOf<List<Emergency>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey, reload) {
        loading = true
        error = null
        try {
            coroutineScope {
                val alertsDeferred = async { api.service.myAlerts() }
                val reportsDeferred = async { api.service.myEmergencies() }
                alerts = alertsDeferred.await()
                reports = reportsDeferred.await()
            }
        } catch (e: Exception) {
            error = api.errorMessage(e)
        } finally {
            loading = false
        }
    }

    Column(modifier = modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "OPERATIONS",
                    color = SensaMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
                Text(
                    "Events",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = SensaAccent
                    )
                } else {
                    IconButton(onClick = { reload++ }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = SensaMuted)
                    }
                }
            }
        }

        error?.let {
            Text(
                it,
                color = SensaDanger,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                end = 18.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionTitle("RESCUE SERVICE ALERTS", "${alerts.size}")
            }
            if (alerts.isEmpty() && !loading) {
                item {
                    EmptyCard(
                        title = "No rescue alerts",
                        text = "Alerts sent by the rescue service will appear here."
                    )
                }
            }
            items(alerts, key = { "alert-${it.id}" }) { alert ->
                AlertCard(alert) { onOpenAlert(alert) }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("MY REPORTS", "${reports.size}")
            }
            if (reports.isEmpty() && !loading) {
                item {
                    EmptyCard(
                        title = "No reports yet",
                        text = "Create a report from the New report tab."
                    )
                }
            }
            items(reports, key = { "report-${it.id}" }) { report ->
                ReportCard(report) { onOpenEmergency(report) }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: SystemAlert, onClick: () -> Unit) {
    SensaCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Campaign,
                contentDescription = null,
                tint = SensaAccent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.padding(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alert.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    alert.content.replace("<br>", "\n"),
                    color = SensaMuted,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(shortDate(alert.createdAt), color = SensaMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ReportCard(emergency: Emergency, onClick: () -> Unit) {
    SensaCard(modifier = Modifier.clickable(onClick = onClick)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = SensaMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.padding(4.dp))
                    Text(
                        emergency.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                StatusBadge(emergency.status)
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = SensaMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.padding(2.dp))
                Text(
                    locationText(emergency),
                    color = SensaMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                "${emergency.files?.size ?: 0} attachments · ${shortDate(emergency.createdAt)}",
                color = SensaMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EmptyCard(title: String, text: String) {
    SensaCard {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(text, color = SensaMuted, fontSize = 12.sp)
    }
}
