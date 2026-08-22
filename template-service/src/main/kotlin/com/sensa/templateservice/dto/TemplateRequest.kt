package com.sensa.templateservice.dto

import com.sensa.templateservice.entity.TemplateChannel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class TemplateRequest(
    @field:NotBlank(message = "Template name is required")
    val templateName: String,

    val description: String? = null,

    @field:NotNull(message = "Channel is required")
    val channel: TemplateChannel,

    @field:NotBlank(message = "Content is required")
    val content: String
)
