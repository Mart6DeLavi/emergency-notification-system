package com.sensa.filesystemservice.dto

data class FileDownloadUrlResponse(
    val url: String,
    val expiresInSeconds: Long
)
