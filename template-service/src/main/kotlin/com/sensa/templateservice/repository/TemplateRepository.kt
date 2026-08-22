package com.sensa.templateservice.repository

import com.sensa.templateservice.entity.Template
import com.sensa.templateservice.entity.TemplateChannel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TemplateRepository : JpaRepository<Template, Long> {
    fun findByTemplateName(templateName: String): Template?
    fun existsByTemplateName(templateName: String): Boolean
    fun findByChannel(channel: TemplateChannel): List<Template>
    fun deleteByTemplateName(templateName: String): Int
}
