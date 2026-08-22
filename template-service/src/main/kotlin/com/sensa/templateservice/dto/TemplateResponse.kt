package com.sensa.templateservice.dto

import com.sensa.templateservice.entity.TemplateChannel
import java.time.LocalDateTime
import java.util.UUID

data class TemplateResponse(
    val id: Long,
    val templateName: String,
    val description: String?,
    val channel: TemplateChannel,
    val content: String,
    val createdBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
