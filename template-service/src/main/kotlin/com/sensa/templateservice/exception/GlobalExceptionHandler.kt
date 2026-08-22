package com.sensa.templateservice.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(TemplateNotFoundException::class)
    fun handleNotFound(ex: TemplateNotFoundException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            mapOf(
                "error" to "Not Found",
                "message" to (ex.message ?: ""),
                "timestamp" to LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(TemplateAlreadyExistsException::class)
    fun handleConflict(ex: TemplateAlreadyExistsException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            mapOf(
                "error" to "Conflict",
                "message" to (ex.message ?: ""),
                "timestamp" to LocalDateTime.now()
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") {
            "${it.field}: ${it.defaultMessage}"
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            mapOf(
                "error" to "Validation Failed",
                "message" to message,
                "timestamp" to LocalDateTime.now()
            )
        )
    }
}
