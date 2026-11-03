package com.sensa.mobile.data

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val country: String,
    val city: String,
    val street: String,
    val homeNumber: String
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String
)

data class NotificationSettings(
    val push: Boolean = true,
    val emailEnabled: Boolean = true,
    val sms: Boolean = false
)

data class UserProfile(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val country: String? = null,
    val city: String? = null,
    val street: String? = null,
    val homeNumber: String? = null,
    val notificationSettings: NotificationSettings? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class UserUpdateRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val country: String? = null,
    val city: String? = null,
    val street: String? = null,
    val homeNumber: String? = null,
    val notificationSettings: NotificationSettings? = null,
    val deviceToken: String? = null
)

data class EmergencyFile(
    val fileId: Long,
    val url: String
)

data class Emergency(
    val id: Long,
    @SerializedName("user_id") val userId: String,
    val title: String,
    val description: String? = null,
    val files: List<EmergencyFile>? = null,
    val country: String? = null,
    val city: String? = null,
    val street: String? = null,
    @SerializedName("alarm_timestamp") val alarmTimestamp: String? = null,
    val status: String,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CreateEmergencyRequest(
    val title: String,
    val description: String? = null,
    val files: List<EmergencyFile>,
    val country: String? = null,
    val city: String? = null,
    val street: String? = null,
    @SerializedName("alarm_timestamp") val alarmTimestamp: String
)

data class FileUploadResult(
    val fileId: Long,
    val url: String? = null,
    val moderationStatus: String,
    val confidencePercentage: Double
) {
    val isApproved: Boolean
        get() = moderationStatus.equals("APPROVED", ignoreCase = true)
}

data class FileLinkRequest(
    @SerializedName("emergencySituationId") val emergencySituationId: Long,
    @SerializedName("fileIds") val fileIds: List<Long>
)

data class FileDownloadUrlResponse(
    val url: String,
    val expiresInSeconds: Long
)

data class SystemAlert(
    val id: String,
    val userId: String? = null,
    val templateName: String? = null,
    val title: String,
    val content: String,
    val channel: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ApiErrorBody(
    val error: String? = null,
    val message: String? = null
)
