package com.sensa.filesystemservice.mapper

import com.sensa.filesystemservice.dto.FileResponse
import com.sensa.filesystemservice.entity.FileEntity
import org.springframework.stereotype.Component

@Component
class FileMapper {

    fun toResponse(entity: FileEntity): FileResponse {
        return FileResponse(
            id = entity.id,
            userId = entity.userId,
            url = entity.url,
            filename = entity.filename,
            emergencySituationId = entity.emergencySituationId,
            moderationStatus = entity.moderationStatus,
            createdAt = entity.createdAt
        )
    }
}
