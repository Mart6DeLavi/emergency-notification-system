package com.sensa.templateservice.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.*

@Document(collection = "templates")
class Template(
    @Id
    var id: String? = null,

    @Indexed(unique = true)
    var templateName: String,

    var description: String? = null,

    var channel: TemplateChannel,

    var content: String,

    var createdBy: UUID,

    @CreatedDate
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
