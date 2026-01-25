package com.xplay.player.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    @Volatile
    private var currentBaseUrl: String? = null

    @Volatile
    private var api: XplayApi? = null

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)  // 连接超时
            .readTimeout(15, TimeUnit.SECONDS)     // 读取超时
            .writeTimeout(15, TimeUnit.SECONDS)    // 写入超时
            .retryOnConnectionFailure(true)        // 连接失败自动重试
            .build()
    }

    fun getApi(baseUrl: String): XplayApi {
        val normalized = normalizeBaseUrl(baseUrl)
        if (api == null || currentBaseUrl != normalized) {
            currentBaseUrl = normalized
            api = Retrofit.Builder()
                .baseUrl(normalized)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(XplayApi::class.java)
        }
        return api!!
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
