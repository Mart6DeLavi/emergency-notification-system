package com.sensa.filesystemservice.service

import com.sensa.filesystemservice.moderation.NsfwDetectionResult
import com.sensa.filesystemservice.moderation.NsfwDetector
import com.sensa.filesystemservice.moderation.VideoFrameExtractor
import com.sensa.filesystemservice.moderation.VideoUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class NsfwModerationService(
    private val nsfwDetector: NsfwDetector,
    private val videoFrameExtractor: VideoFrameExtractor,
    private val videoUtils: VideoUtils
) : ModerationService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun moderateImage(imagePath: Path): NsfwDetectionResult {
        log.info("Moderating image: {}", imagePath.fileName)
        return nsfwDetector.detectFile(imagePath)
    }

    override fun moderateVideo(videoPath: Path, tempDir: Path): NsfwDetectionResult {
        log.info("Moderating video: {}", videoPath.fileName)

        val duration = videoUtils.probeDuration(videoPath)
        log.info("Video duration: {} seconds", duration)

        val framesDir = tempDir.resolve("frames")
        val frames = videoFrameExtractor.extractFrames(videoPath, framesDir)
        log.info("Extracted {} frames for moderation", frames.size)

        for (frame in frames) {
            val result = nsfwDetector.detectFile(frame)
            if (result.nsfw) {
                log.warn("NSFW content detected in video frame: {}", frame.fileName)
                return NsfwDetectionResult(videoPath.toString(), true, result.confidencePercentage)
            }
        }

        return NsfwDetectionResult(videoPath.toString(), false, 0.0)
    }
}
