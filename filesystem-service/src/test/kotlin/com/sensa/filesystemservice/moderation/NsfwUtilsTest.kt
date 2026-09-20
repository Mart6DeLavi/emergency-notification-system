package com.sensa.filesystemservice.moderation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class NsfwUtilsTest {

    @Test
    fun `resizeBufferedImage returns requested dimensions`() {
        val src = BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB)
        val resized = NsfwUtils.resizeBufferedImage(src, 224, 224)

        assertEquals(224, resized.width)
        assertEquals(224, resized.height)
        assertEquals(BufferedImage.TYPE_INT_RGB, resized.type)
    }

    @Test
    fun `extractAndNormalizePixels returns CHW array of correct length`() {
        val img = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        img.setRGB(0, 0, 0x000000)
        img.setRGB(1, 0, 0xFFFFFF)
        img.setRGB(0, 1, 0xFF0000)
        img.setRGB(1, 1, 0x00FF00)

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        val pixels = NsfwUtils.extractAndNormalizePixels(img, mean, std)

        // CHW: 3 channels * 2 * 2 = 12
        assertEquals(12, pixels.size)
    }

    @Test
    fun `softmax sums to one and argmax is correct`() {
        val logits = floatArrayOf(1.0f, 3.0f, 2.0f)
        val probs = NsfwUtils.softmax(logits)

        assertEquals(3, probs.size)
        assertEquals(1.0f, probs.sum(), 1e-5f)
        assertTrue(probs[1] > probs[0])
        assertTrue(probs[1] > probs[2])
    }
}
