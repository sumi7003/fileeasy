package com.xplay.player.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.net.NetworkInterface
import android.os.Build
import java.util.UUID

object DeviceUtils {
    @SuppressLint("HardwareIds")
    fun getSerialNumber(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) 
            ?: UUID.randomUUID().toString()
    }

    /**
     * 获取设备名称，优先级：
     * 1. Settings.Global.DEVICE_NAME (用户在系统设置里取的名字)
     * 2. Settings.Secure.bluetooth_name (蓝牙名称)
     * 3. Build.MANUFACTURER + " " + Build.MODEL (例如 "Samsung SM-G980F")
     */
    fun getSystemDeviceName(context: Context): String {
        // 1. 尝试读取 Global.DEVICE_NAME
        try {
            val deviceName = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            if (!deviceName.isNullOrBlank()) {
                return deviceName
            }
        } catch (e: Exception) {
            // 某些系统可能限制访问或不存在此字段
        }

        // 2. 尝试读取蓝牙名称
        try {
            val btName = Settings.Secure.getString(context.contentResolver, "bluetooth_name")
            if (!btName.isNullOrBlank()) {
                return btName
            }
        } catch (e: Exception) {
            // ignore
        }

        // 3. 回退到 厂商 + 型号
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        if (model.startsWith(manufacturer)) {
            return model.replaceFirstChar { it.uppercase() }
        }
        return "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
    }

    /**
     * 获取本机IP地址的最后一段，例如 "105" (来自 192.168.1.105)
     */
    fun getIpLastSegment(): String {
        return getLocalIpAddress()?.split(".")?.lastOrNull() ?: "?"
    }

    /**
     * 获取本机局域网 IP 地址
     */
    fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filter { !it.isLoopbackAddress && it.hostAddress?.contains(".") == true }
                .mapNotNull { it.hostAddress }
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

