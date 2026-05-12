package com.xplay.player.update

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.xplay.player.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.TreeMap
import java.util.concurrent.TimeUnit

object OtaHttpClient {
    private const val TAG = "OtaHttpClient"
    private const val CLIENT_TYPE = "screenaide"
    private const val SIGN_KEY = "POhY2I8ZZB9k8u6IGv0bRc5ImWYEJwu7"

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun checkVersion(context: Context): OtaVersionData? {
        val params = buildParams(context).toMutableMap()
        params["sign"] = signAfterMd5(params)

        val urlBuilder = BuildConfig.OTA_BASE_URL
            .trimEnd('/')
            .plus("/api/home/client_upgrade")
            .toHttpUrl()
            .newBuilder()

        params.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("XX-Device-Type", CLIENT_TYPE)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "checkVersion failed: HTTP ${response.code}")
                return null
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null
            return parseVersionData(body)
        }
    }

    fun newDownloadClient(): OkHttpClient = client

    private fun buildParams(context: Context): Map<String, String> {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return linkedMapOf(
            "channel_num" to BuildConfig.OTA_CHANNEL,
            "version" to currentVersionCode(packageInfo).toString(),
            "incremental" to Build.VERSION.INCREMENTAL.orEmpty(),
            "release" to Build.VERSION.RELEASE.orEmpty(),
            "security_patch" to currentSecurityPatch(),
            "user" to Build.USER.orEmpty(),
            "fingerprint" to Build.FINGERPRINT.orEmpty(),
            "model" to Build.MODEL.orEmpty(),
            "build_id" to Build.ID.orEmpty(),
            "host" to Build.HOST.orEmpty(),
            "manufacturer" to Build.MANUFACTURER.orEmpty(),
            "product" to Build.PRODUCT.orEmpty(),
            "client_type" to context.packageName,
            "rk_version" to readSystemProperty("ro.product.version"),
            "did" to readSystemProperty("s2.chip.id"),
            "serialno" to readSystemProperty("ro.serialno")
        ).filterValues { it.isNotBlank() }
    }

    private fun parseVersionData(json: String): OtaVersionData? {
        return runCatching {
            val root = gson.fromJson(json, JsonObject::class.java) ?: return null
            val dataElement = root.get("data")
            val versionJson = if (dataElement != null && dataElement.isJsonObject) {
                dataElement.asJsonObject
            } else {
                root
            }
            gson.fromJson(versionJson, OtaVersionData::class.java)
        }.onFailure {
            Log.w(TAG, "Failed to parse version response", it)
        }.getOrNull()
    }

    private fun signAfterMd5(params: Map<String, String>): String {
        val sorted = TreeMap(params)
        val payload = buildString {
            sorted.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    if (isNotEmpty()) append('&')
                    append(key).append('=').append(value)
                }
            }
            append("&key=").append(SIGN_KEY)
        }
        return md5(payload).uppercase()
    }

    private fun md5(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun currentVersionCode(packageInfo: android.content.pm.PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    private fun currentSecurityPatch(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH.orEmpty()
        } else {
            ""
        }
    }

    private fun readSystemProperty(key: String): String {
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java)
            getMethod.invoke(null, key) as? String
        }.getOrNull().orEmpty()
    }
}

