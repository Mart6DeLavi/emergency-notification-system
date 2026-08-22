package com.sensa.templateservice.controller

import com.sensa.templateservice.dto.TemplateRequest
import com.sensa.templateservice.dto.TemplateResponse
import com.sensa.templateservice.entity.TemplateChannel
import com.sensa.templateservice.service.TemplateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/templates")
class TemplateController(
    private val templateService: TemplateService
) {
    @Operation(summary = "Create template", description = "Creates a new notification template")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Template created"),
        ApiResponse(responseCode = "400", description = "Invalid data"),
        ApiResponse(responseCode = "409", description = "Template already exists")
    )
    @PostMapping
    fun create(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody @Valid request: TemplateRequest
    ): ResponseEntity<TemplateResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(templateService.create(request, userId))
    }

    @Operation(summary = "Get all templates", description = "Returns all notification templates")
    @GetMapping
    fun getAll(): ResponseEntity<List<TemplateResponse>> {
        return ResponseEntity.ok(templateService.getAll())
    }

    @Operation(summary = "Get template by name", description = "Returns template by its name")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Template found"),
        ApiResponse(responseCode = "404", description = "Template not found")
    )
    @GetMapping("/{templateName}")
    fun getByTemplateName(
        @PathVariable templateName: String
    ): ResponseEntity<TemplateResponse> {
        return ResponseEntity.ok(templateService.getByTemplateName(templateName))
    }

    @Operation(summary = "Get templates by channel", description = "Returns templates filtered by channel")
    @GetMapping("/channel/{channel}")
    fun getByChannel(
        @PathVariable channel: TemplateChannel
    ): ResponseEntity<List<TemplateResponse>> {
        return ResponseEntity.ok(templateService.getByChannel(channel))
    }

    @Operation(summary = "Update template", description = "Updates existing template by name")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Template updated"),
        ApiResponse(responseCode = "404", description = "Template not found")
    )
    @PatchMapping("/{templateName}")
    fun update(
        @PathVariable templateName: String,
        @RequestBody @Valid request: TemplateRequest
    ): ResponseEntity<TemplateResponse> {
        return ResponseEntity.ok(templateService.update(templateName, request))
    }

    @Operation(summary = "Delete template", description = "Deletes template by name")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Template deleted"),
        ApiResponse(responseCode = "404", description = "Template not found")
    )
    @DeleteMapping("/{templateName}")
    fun delete(@PathVariable templateName: String): ResponseEntity<Void> {
        templateService.delete(templateName)
        return ResponseEntity.noContent().build()
    }
}
