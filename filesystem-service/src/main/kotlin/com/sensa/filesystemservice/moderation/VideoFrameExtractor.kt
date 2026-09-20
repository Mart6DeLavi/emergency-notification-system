package com.sensa.filesystemservice.moderation

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Component
class VideoFrameExtractor {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${video.nsfw.frame-interval-seconds:2}")
    private var frameIntervalSeconds: Int = 2

    @Value("\${video.nsfw.max-frames:10}")
    private var maxFrames: Int = 10

    fun extractFrames(videoPath: Path, outputDir: Path): List<Path> {
        Files.createDirectories(outputDir)
        val outputPattern = outputDir.resolve("frame_%03d.png").toString()

        val command = listOf(
            "ffmpeg", "-y", "-i", videoPath.toString(),
            "-vf", "fps=1/$frameIntervalSeconds",
            "-frames:v", maxFrames.toString(),
            outputPattern
        )

        log.info("Extracting frames with command: {}", command.joinToString(" "))

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val finished = process.waitFor(FFMPEG_TIMEOUT_SEC, TimeUnit.SECONDS)

        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("ffmpeg frame extraction timed out")
        }

        if (process.exitValue() != 0) {
            log.error("ffmpeg output: {}", output)
            throw RuntimeException("ffmpeg frame extraction failed with exit code ${process.exitValue()}")
        }

        return Files.list(outputDir)
            .use { stream ->
                stream.filter { it.fileName.toString().matches(Regex("frame_\\d+\\.png")) }
                    .sorted()
                    .toList()
            }
    }

    companion object {
        private const val FFMPEG_TIMEOUT_SEC = 60L
    }
}
