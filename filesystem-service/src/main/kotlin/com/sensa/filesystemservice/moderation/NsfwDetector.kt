package com.sensa.filesystemservice.moderation

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

@Service
class NsfwDetector(
    private val nsfwModelService: NsfwModelService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${nsfw.enabled:false}")
    private var nsfwEnabled: Boolean = false

    @Value("\${nsfw.local.enabled:true}")
    private var localEnabled: Boolean = true

    fun detectFile(imagePath: Path): NsfwDetectionResult {
        if (!Files.exists(imagePath)) {
            return NsfwDetectionResult(imagePath.toString(), false, 0.0)
        }

        if (!nsfwEnabled) {
            log.debug("NSFW moderation disabled, returning safe")
            return NsfwDetectionResult(imagePath.toString(), false, 0.0)
        }

        if (localEnabled && nsfwModelService.isAvailable()) {
            return nsfwModelService.detectFile(imagePath)
        }

        return NsfwDetectionResult(imagePath.toString(), false, 0.0)
    }
}
