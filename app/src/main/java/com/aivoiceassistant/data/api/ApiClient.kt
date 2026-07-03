package com.aivoiceassistant.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.aivoiceassistant.BuildConfig

object ApiClient {

    // ── Gemini AI Client ─────────────────────────────────────────────────────
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val geminiOkHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Goog-Api-Key", BuildConfig.GEMINI_API_KEY)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(GEMINI_BASE_URL)
        .client(geminiOkHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    // Node.js Backend Base URL from local.properties
    private val PHP_BASE_URL = BuildConfig.BACKEND_BASE_URL






    private val phpOkHttp = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val phpApi: PhpApiService = Retrofit.Builder()
        .baseUrl(PHP_BASE_URL)
        .client(phpOkHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PhpApiService::class.java)
}
