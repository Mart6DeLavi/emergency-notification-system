package com.sensa.filesystemservice.moderation

import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class NsfwModelService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${nsfw.model.path:models/nsfw-detection-model.onnx}")
    private lateinit var modelPath: String

    @Value("\${nsfw.model.input-size:224}")
    private var inputSize: Int = 224

    @Value("\${nsfw.threshold:0.5}")
    private var threshold: Double = 0.5

    @Value("\${nsfw.local.enabled:true}")
    private var localEnabled: Boolean = true

    private var model: ZooModel<Image, FloatArray>? = null
    private var predictor: Predictor<Image, FloatArray>? = null
    private val predictorLock = Object()

    @PostConstruct
    fun init() {
        if (!localEnabled) {
            log.info("NSFW local model disabled")
            return
        }

        val path: Path = Paths.get(modelPath)
        if (!Files.exists(path)) {
            log.error("NSFW model file not found at {}, moderation will skip", modelPath)
            return
        }

        try {
            val modelName = path.fileName.toString().removeSuffix(".onnx")
            val criteria: Criteria<Image, FloatArray> = Criteria.builder()
                .setTypes(Image::class.java, FloatArray::class.java)
                .optModelPath(path.parent)
                .optModelName(modelName)
                .optTranslator(NsfwTranslator(inputSize))
                .optEngine("OnnxRuntime")
                .build()

            model = criteria.loadModel()
            predictor = model?.newPredictor()
            log.info("NSFW model loaded successfully from {}", modelPath)
        } catch (e: Exception) {
            log.error("Failed to load NSFW model, moderation disabled", e)
            localEnabled = false
        }
    }

    fun isAvailable(): Boolean = localEnabled && predictor != null

    fun detectFile(imagePath: Path): NsfwDetectionResult {
        if (!isAvailable()) {
            return NsfwDetectionResult(imagePath.toString(), false, 0.0)
        }

        return try {
            val image: Image = ImageFactory.getInstance().fromFile(imagePath)
            val probs: FloatArray
            synchronized(predictorLock) {
                probs = predictor!!.predict(image)
            }
            val nsfwScore = if (probs.size >= 2) probs[1] else 0.0f
            val isNsfw = nsfwScore.toDouble() >= threshold
            NsfwDetectionResult(imagePath.toString(), isNsfw, nsfwScore.toDouble() * 100)
        } catch (e: Exception) {
            log.warn("NSFW detection failed for {}: {}", imagePath, e.message)
            NsfwDetectionResult(imagePath.toString(), false, 0.0)
        }
    }

    @PreDestroy
    fun cleanup() {
        try {
            predictor?.close()
            model?.close()
        } catch (e: Exception) {
            log.warn("Error closing NSFW model", e)
        }
    }
}
