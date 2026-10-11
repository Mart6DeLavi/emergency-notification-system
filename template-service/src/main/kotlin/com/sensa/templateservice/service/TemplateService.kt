package com.sensa.templateservice.service

import com.sensa.templateservice.dto.TemplateRequest
import com.sensa.templateservice.dto.TemplateResponse
import com.sensa.templateservice.entity.TemplateChannel
import com.sensa.templateservice.exception.TemplateAlreadyExistsException
import com.sensa.templateservice.exception.TemplateNotFoundException
import com.sensa.templateservice.mapper.TemplateMapper
import com.sensa.templateservice.repository.TemplateRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TemplateService(
    private val templateRepository: TemplateRepository,
    private val templateMapper: TemplateMapper
) {
    fun create(request: TemplateRequest, createdBy: UUID): TemplateResponse {
        if (templateRepository.existsByTemplateName(request.templateName)) {
            throw TemplateAlreadyExistsException(request.templateName)
        }
        val entity = templateMapper.toEntity(request, createdBy)
        return templateMapper.toResponse(templateRepository.save(entity))
    }

    @Cacheable(cacheNames = ["templates"], key = "#templateName")
    fun getByTemplateName(templateName: String): TemplateResponse {
        val entity = templateRepository.findByTemplateName(templateName)
            ?: throw TemplateNotFoundException(templateName)
        return templateMapper.toResponse(entity)
    }

    fun getAll(): List<TemplateResponse> {
        return templateRepository.findAll().map { templateMapper.toResponse(it) }
    }

    fun getByChannel(channel: TemplateChannel): List<TemplateResponse> {
        return templateRepository.findByChannel(channel).map { templateMapper.toResponse(it) }
    }

    @CacheEvict(cacheNames = ["templates"], key = "#templateName")
    fun update(templateName: String, request: TemplateRequest): TemplateResponse {
        val existing = templateRepository.findByTemplateName(templateName)
            ?: throw TemplateNotFoundException(templateName)
        templateMapper.updateEntity(existing, request)
        return templateMapper.toResponse(templateRepository.save(existing))
    }

    @CacheEvict(cacheNames = ["templates"], key = "#templateName")
    fun delete(templateName: String) {
        val rows = templateRepository.deleteByTemplateName(templateName)
        if (rows == 0L) throw TemplateNotFoundException(templateName)
    }
}
