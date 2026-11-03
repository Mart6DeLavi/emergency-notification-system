package com.sensa.mobile.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class SessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sensa_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("token", null)
        private set(value) = prefs.edit().putString("token", value).apply()

    var userId: String?
        get() = prefs.getString("userId", null)
        private set(value) = prefs.edit().putString("userId", value).apply()

    var email: String?
        get() = prefs.getString("email", null)
        private set(value) = prefs.edit().putString("email", value).apply()

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank() && !userId.isNullOrBlank()

    fun save(auth: AuthResponse) {
        token = auth.token
        userId = auth.userId
        email = auth.email
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

class ApiClient(private val session: SessionStore) {

    private val gson = Gson()

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            session.token?.let { builder.header("Authorization", "Bearer $it") }
            chain.proceed(builder.build())
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    val service: ApiService = Retrofit.Builder()
        .baseUrl("https://panel.sensanotify.com/api/")
        .client(http)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)

    fun errorMessage(error: Throwable): String {
        return when (error) {
            is HttpException -> {
                val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
                val parsed = body?.let {
                    runCatching { gson.fromJson(it, ApiErrorBody::class.java) }.getOrNull()
                }
                parsed?.message ?: parsed?.error ?: "Request failed (${error.code()})"
            }

            is IOException -> "Network error: ${error.message ?: "check your connection"}"
            is JsonSyntaxException -> "Server returned an unexpected response."
            else -> error.message ?: "Something went wrong."
        }
    }
}
