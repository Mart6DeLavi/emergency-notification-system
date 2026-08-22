package com.sensa.templateservice.service

import com.sensa.templateservice.dto.TemplateRequest
import com.sensa.templateservice.dto.TemplateResponse
import com.sensa.templateservice.entity.Template
import com.sensa.templateservice.entity.TemplateChannel
import com.sensa.templateservice.exception.TemplateAlreadyExistsException
import com.sensa.templateservice.exception.TemplateNotFoundException
import com.sensa.templateservice.mapper.TemplateMapper
import com.sensa.templateservice.repository.TemplateRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.UUID

class TemplateServiceTest {

    private lateinit var templateRepository: TemplateRepository
    private lateinit var templateMapper: TemplateMapper
    private lateinit var templateService: TemplateService

    private val userId = UUID.randomUUID()
    private val now = LocalDateTime.now()

    @BeforeEach
    fun setUp() {
        templateRepository = Mockito.mock(TemplateRepository::class.java)
        templateMapper = Mockito.mock(TemplateMapper::class.java)
        templateService = TemplateService(templateRepository, templateMapper)
    }

    private fun buildEntity(
        id: Long = 1L,
        templateName: String = "alert-template",
        description: String? = "Emergency alert",
        channel: TemplateChannel = TemplateChannel.PUSH,
        content: String = "Alert: {{message}}",
        createdBy: UUID = userId
    ) = Template(
        id = id,
        templateName = templateName,
        description = description,
        channel = channel,
        content = content,
        createdBy = createdBy,
        createdAt = now,
        updatedAt = now
    )

    private fun buildRequest(
        templateName: String = "alert-template",
        description: String? = "Emergency alert",
        channel: TemplateChannel = TemplateChannel.PUSH,
        content: String = "Alert: {{message}}"
    ) = TemplateRequest(
        templateName = templateName,
        description = description,
        channel = channel,
        content = content
    )

    private fun buildResponse(
        id: Long = 1L,
        templateName: String = "alert-template",
        description: String? = "Emergency alert",
        channel: TemplateChannel = TemplateChannel.PUSH,
        content: String = "Alert: {{message}}"
    ) = TemplateResponse(
        id = id,
        templateName = templateName,
        description = description,
        channel = channel,
        content = content,
        createdBy = userId,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `create template success`() {
        val request = buildRequest()
        val entity = buildEntity()
        val response = buildResponse()

        Mockito.`when`(templateRepository.existsByTemplateName(request.templateName)).thenReturn(false)
        Mockito.`when`(templateMapper.toEntity(request, userId)).thenReturn(entity)
        Mockito.`when`(templateRepository.save(entity)).thenReturn(entity)
        Mockito.`when`(templateMapper.toResponse(entity)).thenReturn(response)

        val result = templateService.create(request, userId)

        assertNotNull(result)
        assertEquals("alert-template", result.templateName)
        Mockito.verify(templateRepository).save(entity)
    }

    @Test
    fun `create template already exists throws exception`() {
        val request = buildRequest()
        Mockito.`when`(templateRepository.existsByTemplateName(request.templateName)).thenReturn(true)

        assertThrows(TemplateAlreadyExistsException::class.java) {
            templateService.create(request, userId)
        }
    }

    @Test
    fun `get by template name success`() {
        val entity = buildEntity()
        val response = buildResponse()

        Mockito.`when`(templateRepository.findByTemplateName("alert-template")).thenReturn(entity)
        Mockito.`when`(templateMapper.toResponse(entity)).thenReturn(response)

        val result = templateService.getByTemplateName("alert-template")

        assertEquals("alert-template", result.templateName)
    }

    @Test
    fun `get by template name not found throws exception`() {
        Mockito.`when`(templateRepository.findByTemplateName("unknown")).thenReturn(null)

        assertThrows(TemplateNotFoundException::class.java) {
            templateService.getByTemplateName("unknown")
        }
    }

    @Test
    fun `get all returns list`() {
        val entity = buildEntity()
        val response = buildResponse()

        Mockito.`when`(templateRepository.findAll()).thenReturn(listOf(entity))
        Mockito.`when`(templateMapper.toResponse(entity)).thenReturn(response)

        val result = templateService.getAll()

        assertEquals(1, result.size)
    }

    @Test
    fun `get by channel returns filtered list`() {
        val entity = buildEntity()
        val response = buildResponse()

        Mockito.`when`(templateRepository.findByChannel(TemplateChannel.PUSH)).thenReturn(listOf(entity))
        Mockito.`when`(templateMapper.toResponse(entity)).thenReturn(response)

        val result = templateService.getByChannel(TemplateChannel.PUSH)

        assertEquals(1, result.size)
    }

    @Test
    fun `update template success`() {
        val existing = buildEntity()
        val request = buildRequest(templateName = "updated-template")
        val response = buildResponse(templateName = "updated-template")

        Mockito.`when`(templateRepository.findByTemplateName("alert-template")).thenReturn(existing)
        Mockito.`when`(templateRepository.save(existing)).thenReturn(existing)
        Mockito.`when`(templateMapper.toResponse(existing)).thenReturn(response)

        val result = templateService.update("alert-template", request)

        assertNotNull(result)
        assertEquals("updated-template", result.templateName)
        Mockito.verify(templateMapper).updateEntity(existing, request)
    }

    @Test
    fun `update template not found throws exception`() {
        val request = buildRequest()
        Mockito.`when`(templateRepository.findByTemplateName("unknown")).thenReturn(null)

        assertThrows(TemplateNotFoundException::class.java) {
            templateService.update("unknown", request)
        }
    }

    @Test
    fun `delete template success`() {
        Mockito.`when`(templateRepository.deleteByTemplateName("alert-template")).thenReturn(1)

        assertDoesNotThrow { templateService.delete("alert-template") }
    }

    @Test
    fun `delete template not found throws exception`() {
        Mockito.`when`(templateRepository.deleteByTemplateName("unknown")).thenReturn(0)

        assertThrows(TemplateNotFoundException::class.java) {
            templateService.delete("unknown")
        }
    }
}
