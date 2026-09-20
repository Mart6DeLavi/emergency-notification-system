package com.sensa.filesystemservice.moderation

import ai.djl.ndarray.NDList
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext

class NsfwTranslator(
    private val imageSize: Int
) : Translator<ai.djl.modality.cv.Image, FloatArray> {

    companion object {
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    override fun processInput(ctx: TranslatorContext, input: ai.djl.modality.cv.Image): NDList {
        val original = input.wrappedImage as java.awt.image.BufferedImage
        val resized = NsfwUtils.resizeBufferedImage(original, imageSize, imageSize)
        val pixels = NsfwUtils.extractAndNormalizePixels(resized, MEAN, STD)
        val array = ctx.ndManager.create(pixels, ai.djl.ndarray.types.Shape(1, 3, imageSize.toLong(), imageSize.toLong()))
        return NDList(array)
    }

    override fun processOutput(ctx: TranslatorContext, list: NDList): FloatArray {
        val array = list.singletonOrThrow()
        val logits = array.toFloatArray()
        return NsfwUtils.softmax(logits)
    }
}
