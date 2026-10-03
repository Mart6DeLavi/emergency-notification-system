package com.sensa.filesystemservice.dto

data class FileLinkRequest(
    val emergencySituationId: Long,
    val fileIds: List<Long>
)
