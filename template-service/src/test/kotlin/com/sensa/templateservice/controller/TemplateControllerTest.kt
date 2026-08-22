package com.sensa.templateservice.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sensa.templateservice.dto.TemplateResponse
import com.sensa.templateservice.entity.TemplateChannel
import com.sensa.templateservice.service.TemplateService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TemplateControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var templateService: TemplateService

    private val userId = UUID.randomUUID()
    private val now = LocalDateTime.now()
    private lateinit var jwtToken: String
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        val key: SecretKey = Keys.hmacShaKeyFor(
            "test-secret-key-for-template-service-testing-256bit".toByteArray(StandardCharsets.UTF_8)
        )
        jwtToken = Jwts.builder()
            .subject("test@example.com")
            .claim("userId", userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(key)
            .compact()
    }

    private fun makeResponse(
        templateName: String = "alert-template",
        channel: TemplateChannel = TemplateChannel.PUSH
    ) = TemplateResponse(
        id = 1L,
        templateName = templateName,
        description = "Test description",
        channel = channel,
        content = "Test content",
        createdBy = userId,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `get all templates should return 200`() {
        Mockito.`when`(templateService.getAll()).thenReturn(listOf(makeResponse()))

        mockMvc.perform(
            get("/api/v1/templates")
                .header("Authorization", "Bearer $jwtToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].templateName").value("alert-template"))
    }

    @Test
    fun `get template by name should return 200`() {
        Mockito.`when`(templateService.getByTemplateName("alert-template")).thenReturn(makeResponse())

        mockMvc.perform(
            get("/api/v1/templates/alert-template")
                .header("Authorization", "Bearer $jwtToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.templateName").value("alert-template"))
    }

    @Test
    fun `get template by name not found should return 404`() {
        Mockito.`when`(templateService.getByTemplateName("unknown"))
            .thenThrow(com.sensa.templateservice.exception.TemplateNotFoundException("unknown"))

        mockMvc.perform(
            get("/api/v1/templates/unknown")
                .header("Authorization", "Bearer $jwtToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `get templates by channel should return 200`() {
        Mockito.`when`(templateService.getByChannel(TemplateChannel.PUSH))
            .thenReturn(listOf(makeResponse()))

        mockMvc.perform(
            get("/api/v1/templates/channel/PUSH")
                .header("Authorization", "Bearer $jwtToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].channel").value("PUSH"))
    }

    @Test
    fun `delete template should return 204`() {
        mockMvc.perform(
            delete("/api/v1/templates/alert-template")
                .header("Authorization", "Bearer $jwtToken")
        )
            .andExpect(status().isNoContent)
    }
}
