package com.sensa.filesystemservice.moderation

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object NsfwUtils {

    fun resizeBufferedImage(original: BufferedImage, width: Int, height: Int): BufferedImage {
        val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = resized.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(original, 0, 0, width, height, null)
        g.dispose()
        return resized
    }

    fun extractAndNormalizePixels(
        image: BufferedImage,
        mean: FloatArray,
        std: FloatArray
    ): FloatArray {
        val width = image.width
        val height = image.height
        val pixels = FloatArray(3 * width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16 and 0xFF) / 255f
                val g = (rgb shr 8 and 0xFF) / 255f
                val b = (rgb and 0xFF) / 255f

                val idx = y * width + x
                pixels[idx] = (r - mean[0]) / std[0]
                pixels[width * height + idx] = (g - mean[1]) / std[1]
                pixels[2 * width * height + idx] = (b - mean[2]) / std[2]
            }
        }
        return pixels
    }

    fun softmax(input: FloatArray): FloatArray {
        var max = Float.NEGATIVE_INFINITY
        for (v in input) {
            if (v > max) max = v
        }
        var sum = 0.0
        val result = FloatArray(input.size)
        for (i in input.indices) {
            result[i] = Math.exp((input[i] - max).toDouble()).toFloat()
            sum += result[i]
        }
        for (i in result.indices) {
            result[i] = (result[i] / sum).toFloat()
        }
        return result
    }

    fun loadImage(path: java.nio.file.Path): BufferedImage {
        return ImageIO.read(path.toFile())
    }
}
