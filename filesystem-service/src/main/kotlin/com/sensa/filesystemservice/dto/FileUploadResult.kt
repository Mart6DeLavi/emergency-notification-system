package com.sensa.filesystemservice.dto

import com.sensa.filesystemservice.entity.ModerationStatus

data class FileUploadResult(
    val fileId: Long,
    val url: String?,
    val moderationStatus: ModerationStatus,
    val confidencePercentage: Double
)
