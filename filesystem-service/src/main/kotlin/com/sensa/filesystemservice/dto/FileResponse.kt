package com.sensa.filesystemservice.dto

import com.sensa.filesystemservice.entity.ModerationStatus
import java.time.LocalDateTime
import java.util.UUID

data class FileResponse(
    val id: Long,
    val userId: UUID,
    val url: String,
    val filename: String,
    val emergencySituationId: Long?,
    val moderationStatus: ModerationStatus,
    val createdAt: LocalDateTime
)
