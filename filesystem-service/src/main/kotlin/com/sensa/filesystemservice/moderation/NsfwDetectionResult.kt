package com.sensa.filesystemservice.moderation

data class NsfwDetectionResult(
    val filePath: String,
    val nsfw: Boolean,
    val confidencePercentage: Double
) {
    constructor(nsfw: Boolean, confidencePercentage: Double) : this("", nsfw, confidencePercentage)
}
