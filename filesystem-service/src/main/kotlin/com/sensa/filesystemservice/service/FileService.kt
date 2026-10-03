package com.sensa.filesystemservice.service

import com.sensa.filesystemservice.dto.FileLinkRequest
import com.sensa.filesystemservice.dto.FileModeratedEvent
import com.sensa.filesystemservice.dto.FileResponse
import com.sensa.filesystemservice.dto.FileUploadResult
import com.sensa.filesystemservice.entity.FileEntity
import com.sensa.filesystemservice.entity.ModerationStatus
import com.sensa.filesystemservice.mapper.FileMapper
import com.sensa.filesystemservice.moderation.NsfwDetectionResult
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

    fun uploadFiles(files: List<MultipartFile>, userId: UUID): List<FileUploadResult> {
        log.info("Uploading {} file(s) for userId={}", files.size, userId)
        return files.map { file -> processFile(file, userId) }
    }

    private fun processFile(file: MultipartFile, userId: UUID): FileUploadResult {
        val extension = resolveExtension(file)
        validateContentType(file, extension)

        val tempFile = Files.createTempFile("upload_", ".$extension")
        val framesDir = Files.createTempDirectory("frames_")
        try {
            file.transferTo(tempFile.toFile())

            val result = if (isVideo(extension)) {
                moderationService.moderateVideo(tempFile, framesDir)
            } else {
                moderationService.moderateImage(tempFile)
            }

            return if (result.nsfw) {
                persistRejected(file, userId, extension, result)
            } else {
                persistApproved(file, userId, extension, tempFile, result)
            }
        } finally {
            deleteRecursively(framesDir)
            Files.deleteIfExists(tempFile)
        }
    }

    private fun persistRejected(
        file: MultipartFile,
        userId: UUID,
        extension: String,
        result: NsfwDetectionResult
    ): FileUploadResult {
        log.warn("NSFW content rejected: filename={}, confidence={}%", file.originalFilename, result.confidencePercentage)

        val entity = FileEntity(
            userId = userId,
            url = null,
            filename = file.originalFilename ?: "$userId/file.${extension}",
            emergencySituationId = null,
            moderationStatus = ModerationStatus.REJECTED
        )
        val saved = fileRepository.save(entity)
        emitModeratedEvent(saved, ModerationStatus.REJECTED)

        return FileUploadResult(
            fileId = saved.id,
            url = null,
            moderationStatus = ModerationStatus.REJECTED,
            confidencePercentage = result.confidencePercentage
        )
    }

    private fun persistApproved(
        file: MultipartFile,
        userId: UUID,
        extension: String,
        tempFile: Path,
        result: NsfwDetectionResult
    ): FileUploadResult {
        val key = "$userId/${UUID.randomUUID()}.$extension"
        val url = uploadToS3(tempFile, key, file.contentType)

        val entity = FileEntity(
            userId = userId,
            url = url,
            filename = file.originalFilename ?: key,
            emergencySituationId = null,
            moderationStatus = ModerationStatus.APPROVED
        )
        val saved = fileRepository.save(entity)
        emitModeratedEvent(saved, ModerationStatus.APPROVED)

        log.info("File uploaded and approved: id={}, url={}", saved.id, saved.url)

        return FileUploadResult(
            fileId = saved.id,
            url = saved.url,
            moderationStatus = ModerationStatus.APPROVED,
            confidencePercentage = result.confidencePercentage
        )
    }

    private fun emitModeratedEvent(entity: FileEntity, status: ModerationStatus) {
        val event = FileModeratedEvent(
            fileId = entity.id,
            userId = entity.userId,
            moderateResult = status.name,
            emergencySituationId = entity.emergencySituationId
        )
        kafkaTemplate.send(fileModeratedTopic, entity.id.toString(), event)
    }

    @Transactional
    fun linkFiles(request: FileLinkRequest, userId: UUID): List<FileResponse> {
        log.info("Linking {} file(s) to emergency {}", request.fileIds.size, request.emergencySituationId)

        val files = fileRepository.findAllByIdInAndUserId(request.fileIds, userId)
        val foundIds = files.map { it.id }.toSet()
        val missingIds = request.fileIds.filterNot { foundIds.contains(it) }

        if (missingIds.isNotEmpty()) {
            throw NoSuchElementException("Files not found: $missingIds")
        }

        files.forEach { it.emergencySituationId = request.emergencySituationId }
        val saved = fileRepository.saveAll(files)

        return saved.map { fileMapper.toResponse(it) }
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

        entity.url?.let { deleteFromS3(it) }

        val deleted = fileRepository.deleteByIdAndUserId(id, userId)
        if (deleted == 0) {
            throw NoSuchElementException("File with id $id not found")
        }

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

    private fun deleteRecursively(dir: Path) {
        if (!Files.exists(dir)) return
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
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
