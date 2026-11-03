package com.sensa.mobile.data

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("v1/users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): UserProfile

    @PATCH("v1/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body request: UserUpdateRequest
    ): UserProfile

    @GET("v1/emergencies/my")
    suspend fun myEmergencies(): List<Emergency>

    @GET("v1/emergencies/{id}")
    suspend fun getEmergency(@Path("id") id: Long): Emergency

    @POST("v1/emergencies")
    suspend fun createEmergency(@Body request: CreateEmergencyRequest): Emergency

    @Multipart
    @POST("v1/files")
    suspend fun uploadFiles(@Part files: List<MultipartBody.Part>): List<FileUploadResult>

    @PATCH("v1/files/link")
    suspend fun linkFiles(@Body request: FileLinkRequest): Response<Unit>

    @GET("v1/files/{id}/url")
    suspend fun getFileUrl(@Path("id") id: Long): FileDownloadUrlResponse

    @GET("v1/notifications")
    suspend fun myAlerts(): List<SystemAlert>
}
