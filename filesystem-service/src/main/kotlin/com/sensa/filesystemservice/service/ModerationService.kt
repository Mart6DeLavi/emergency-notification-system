package com.sensa.filesystemservice.service

import com.sensa.filesystemservice.moderation.NsfwDetectionResult
import java.nio.file.Path

interface ModerationService {
    fun moderateImage(imagePath: Path): NsfwDetectionResult
    fun moderateVideo(videoPath: Path, tempDir: Path): NsfwDetectionResult
}
