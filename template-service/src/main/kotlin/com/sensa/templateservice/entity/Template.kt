package com.sensa.templateservice.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "templates")
class Template(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "template_name", nullable = false, unique = true)
    var templateName: String,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "channel", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    var channel: TemplateChannel,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "created_by", nullable = false)
    var createdBy: UUID,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
