package com.sensa.templateservice.mapper

import com.sensa.templateservice.dto.TemplateRequest
import com.sensa.templateservice.dto.TemplateResponse
import com.sensa.templateservice.entity.Template
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class TemplateMapper {

    fun toEntity(request: TemplateRequest, createdBy: UUID): Template = Template(
        templateName = request.templateName,
        description = request.description,
        channel = request.channel,
        content = request.content,
        createdBy = createdBy
    )

    fun toResponse(entity: Template): TemplateResponse = TemplateResponse(
        id = entity.id,
        templateName = entity.templateName,
        description = entity.description,
        channel = entity.channel,
        content = entity.content,
        createdBy = entity.createdBy,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun updateEntity(existing: Template, request: TemplateRequest) {
        existing.templateName = request.templateName
        existing.description = request.description
        existing.channel = request.channel
        existing.content = request.content
        existing.updatedAt = LocalDateTime.now()
    }
}
