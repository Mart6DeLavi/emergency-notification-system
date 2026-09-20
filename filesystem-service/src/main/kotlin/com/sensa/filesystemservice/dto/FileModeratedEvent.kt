package com.sensa.filesystemservice.dto

import java.util.UUID

data class FileModeratedEvent(
    val fileId: Long,
    val userId: UUID,
    val moderateResult: String,
    val emergencySituationId: Long?
)
