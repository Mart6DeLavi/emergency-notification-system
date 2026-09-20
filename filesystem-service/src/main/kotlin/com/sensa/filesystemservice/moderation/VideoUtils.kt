package com.sensa.filesystemservice.moderation

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Component
class VideoUtils {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${video.max-duration-seconds:30}")
    private var maxDurationSeconds: Int = 30

    fun probeDuration(videoPath: Path): Double {
        val output = runFfprobe(
            videoPath,
            listOf(
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1"
            )
        )

        val duration = output.trim().toDoubleOrNull()
            ?: throw RuntimeException("Could not parse video duration")

        if (duration > maxDurationSeconds) {
            throw VideoTooLongException("Video duration $duration exceeds max $maxDurationSeconds seconds")
        }

        return duration
    }

    private fun runFfprobe(videoPath: Path, args: List<String>): String {
        val command = listOf("ffprobe") + args + videoPath.toString()

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val finished = process.waitFor(FFPROBE_TIMEOUT_SEC, TimeUnit.SECONDS)

        if (!finished) {
            process.destroyForcibly()
            throw RuntimeException("ffprobe timed out")
        }

        if (process.exitValue() != 0) {
            log.error("ffprobe output: {}", output)
            throw RuntimeException("ffprobe failed with exit code ${process.exitValue()}")
        }

        return output
    }

    companion object {
        private const val FFPROBE_TIMEOUT_SEC = 30L
    }
}

class VideoTooLongException(message: String) : RuntimeException(message)
