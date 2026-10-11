package com.sensa.templateservice.repository

import com.sensa.templateservice.entity.Template
import com.sensa.templateservice.entity.TemplateChannel
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TemplateRepository : MongoRepository<Template, String> {
    fun findByTemplateName(templateName: String): Template?
    fun existsByTemplateName(templateName: String): Boolean
    fun findByChannel(channel: TemplateChannel): List<Template>
    fun deleteByTemplateName(templateName: String): Long
}
