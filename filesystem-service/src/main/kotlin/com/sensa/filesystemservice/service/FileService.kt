package com.sensa.filesystemservice.service

import com.sensa.filesystemservice.dto.FileModeratedEvent
import com.sensa.filesystemservice.dto.FileResponse
import com.sensa.filesystemservice.entity.FileEntity
import com.sensa.filesystemservice.entity.ModerationStatus
import com.sensa.filesystemservice.exception.NsfwContentException
import com.sensa.filesystemservice.mapper.FileMapper
import com.sensa.filesystemservice.repository.FileRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Service
class FileService(
    private val fileRepository: FileRepository,
    private val fileMapper: FileMapper,
    private val moderationService: ModerationService,
    private val s3Client: S3Client,
    private val kafkaTemplate: KafkaTemplate<String, FileModeratedEvent>
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${aws.s3.bucket}")
    private lateinit var bucket: String

    @Value("\${aws.region:eu-west-1}")
    private lateinit var region: String

    @Value("\${spring.kafka.topics.file-moderated:file.moderated}")
    private lateinit var fileModeratedTopic: String

    @Transactional
    fun upload(file: MultipartFile, userId: UUID, emergencySituationId: Long?): FileResponse {
        log.info("Uploading file: filename={}, userId={}", file.originalFilename, userId)

        val extension = resolveExtension(file)
        validateContentType(file, extension)

        val tempFile = Files.createTempFile("upload_", ".$extension")
        try {
            file.transferTo(tempFile.toFile())

            val key = "$userId/${UUID.randomUUID()}.$extension"
            val result = if (isVideo(extension)) {
                moderationService.moderateVideo(tempFile, tempFile.parent)
            } else {
                moderationService.moderateImage(tempFile)
            }

            if (result.nsfw) {
                log.warn("NSFW content rejected: filename={}", file.originalFilename)
                throw NsfwContentException(
                    "File contains inappropriate content (confidence: %.1f%%)".format(result.confidencePercentage)
                )
            }

            val url = uploadToS3(tempFile, key, file.contentType)

            val entity = FileEntity(
                userId = userId,
                url = url,
                filename = file.originalFilename ?: key,
                emergencySituationId = emergencySituationId,
                moderationStatus = ModerationStatus.APPROVED
            )

            val saved = fileRepository.save(entity)

            val event = FileModeratedEvent(
                fileId = saved.id,
                userId = saved.userId,
                moderateResult = "APPROVED",
                emergencySituationId = saved.emergencySituationId
            )
            kafkaTemplate.send(fileModeratedTopic, saved.id.toString(), event)
            log.info("File uploaded and approved: id={}, url={}", saved.id, saved.url)

            return fileMapper.toResponse(saved)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    fun getFile(id: Long, userId: UUID): FileResponse {
        val entity = fileRepository.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("File with id $id not found")
        return fileMapper.toResponse(entity)
    }

    @Transactional
    fun deleteFile(id: Long, userId: UUID) {
        val entity = fileRepository.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("File with id $id not found")

        val deleted = fileRepository.deleteByIdAndUserId(id, userId)
        if (deleted == 0) {
            throw NoSuchElementException("File with id $id not found")
        }

        deleteFromS3(entity.url)
        log.info("File deleted: id={}", id)
    }

    private fun uploadToS3(tempFile: Path, key: String, contentType: String?): String {
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build()

        s3Client.putObject(request, RequestBody.fromFile(tempFile))

        return "https://$bucket.s3.$region.amazonaws.com/$key"
    }

    private fun deleteFromS3(url: String) {
        val key = url.substringAfter("$bucket.s3.$region.amazonaws.com/")
        if (key.isBlank()) return
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
    }

    private fun resolveExtension(file: MultipartFile): String {
        val filename = file.originalFilename ?: ""
        val ext = filename.substringAfterLast('.', "").lowercase()
        if (ext.isBlank()) {
            throw IllegalArgumentException("File must have an extension")
        }
        return ext
    }

    private fun validateContentType(file: MultipartFile, extension: String) {
        val isImage = IMAGE_EXTENSIONS.contains(extension)
        val isVideoFile = VIDEO_EXTENSIONS.contains(extension)

        if (!isImage && !isVideoFile) {
            throw IllegalArgumentException("Unsupported file type: .$extension")
        }
    }

    private fun isVideo(extension: String): Boolean = VIDEO_EXTENSIONS.contains(extension)

    companion object {
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "svg")
        private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mov", "avi", "mkv")
    }
}
