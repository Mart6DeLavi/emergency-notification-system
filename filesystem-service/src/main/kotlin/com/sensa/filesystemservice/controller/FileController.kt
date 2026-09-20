package com.sensa.filesystemservice.controller

import com.sensa.filesystemservice.dto.FileResponse
import com.sensa.filesystemservice.service.FileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/files")
class FileController(
    private val fileService: FileService
) {

    @Operation(summary = "Upload a file", description = "Uploads a file to S3 and runs NSFW moderation")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "File uploaded and approved"),
        ApiResponse(responseCode = "400", description = "Unsupported type or NSFW content"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "500", description = "Server error")
    )
    @PostMapping
    fun upload(
        @AuthenticationPrincipal userId: UUID,
        @RequestPart("file") file: MultipartFile,
        @RequestParam(name = "emergencySituationId", required = false) emergencySituationId: Long?
    ): ResponseEntity<FileResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(fileService.upload(file, userId, emergencySituationId))
    }

    @Operation(summary = "Get file metadata", description = "Returns file metadata by id")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "File found"),
        ApiResponse(responseCode = "404", description = "File not found")
    )
    @GetMapping("/{id}")
    fun getFile(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable id: Long
    ): ResponseEntity<FileResponse> {
        return ResponseEntity.ok(fileService.getFile(id, userId))
    }

    @Operation(summary = "Delete a file", description = "Deletes file metadata and object from S3")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "File deleted"),
        ApiResponse(responseCode = "404", description = "File not found")
    )
    @DeleteMapping("/{id}")
    fun deleteFile(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        fileService.deleteFile(id, userId)
        return ResponseEntity.noContent().build()
    }
}
