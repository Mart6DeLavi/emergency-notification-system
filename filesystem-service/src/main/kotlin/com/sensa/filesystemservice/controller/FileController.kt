package com.sensa.filesystemservice.controller

import com.sensa.filesystemservice.dto.FileLinkRequest
import com.sensa.filesystemservice.dto.FileResponse
import com.sensa.filesystemservice.dto.FileUploadResult
import com.sensa.filesystemservice.service.FileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/files")
class FileController(
    private val fileService: FileService
) {

    @Operation(summary = "Upload files", description = "Uploads files to S3 and runs NSFW moderation per file")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Files processed (approved or rejected)"),
        ApiResponse(responseCode = "400", description = "Unsupported file type"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "500", description = "Server error")
    )
    @PostMapping
    fun upload(
        @AuthenticationPrincipal userId: UUID,
        @RequestPart("files") files: List<MultipartFile>
    ): ResponseEntity<List<FileUploadResult>> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(fileService.uploadFiles(files, userId))
    }

    @Operation(summary = "Link files to emergency", description = "Sets emergency_situation_id on a list of files")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Files linked"),
        ApiResponse(responseCode = "404", description = "One or more files not found")
    )
    @PatchMapping("/link")
    fun linkFiles(
        @AuthenticationPrincipal userId: UUID,
        @RequestBody request: FileLinkRequest
    ): ResponseEntity<List<FileResponse>> {
        return ResponseEntity.ok(fileService.linkFiles(request, userId))
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
