package com.xplay.player.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

data class LanAccessInfo(
    val host: String? = null,
    val source: LanAddressSource = LanAddressSource.UNAVAILABLE,
    val networkName: String? = null,
    val interfaceName: String? = null
) {
    val isAvailable: Boolean
        get() = !host.isNullOrBlank()

    val uploadUrl: String?
        get() = host?.let { "http://$it:3000/" }

    val adminUrl: String?
        get() = host?.let { "http://$it:3000/admin" }
}

enum class LanAddressSource {
    WIFI,
    HOTSPOT,
    OTHER,
    UNAVAILABLE
}

object LanAddressResolver {
    private val hotspotInterfacePrefixes = listOf("ap", "swlan", "softap", "rndis", "usb", "wlan1")
    private val ignoredInterfacePrefixes = listOf("lo", "rmnet", "ccmni", "dummy", "tun", "tap", "veth")
    private const val WIFI_AP_STATE_ENABLED_FALLBACK = 13

    fun resolve(context: Context): LanAccessInfo {
        resolveActiveWifiAddress(context)?.let { return it }

        val candidates = getInterfaceCandidates()
        if (readHotspotEnabledState(context) != false) {
            candidates.firstOrNull { it.isHotspot }?.let { candidate ->
                return LanAccessInfo(
                    host = candidate.host,
                    source = LanAddressSource.HOTSPOT,
                    networkName = "共享热点",
                    interfaceName = candidate.interfaceName
                )
            }
        }

        candidates.firstOrNull()?.let { candidate ->
            return LanAccessInfo(
                host = candidate.host,
                source = LanAddressSource.OTHER,
                networkName = candidate.interfaceName,
                interfaceName = candidate.interfaceName
            )
        }

        return LanAccessInfo()
    }

    private fun resolveActiveWifiAddress(context: Context): LanAccessInfo? {
        val connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null

        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null
        val host = getIpv4Address(linkProperties) ?: return null

        return LanAccessInfo(
            host = host,
            source = LanAddressSource.WIFI,
            networkName = readWifiSsid(context),
            interfaceName = linkProperties.interfaceName
        )
    }

    private fun getIpv4Address(linkProperties: LinkProperties): String? {
        return linkProperties.linkAddresses
            .asSequence()
            .mapNotNull { it.address as? Inet4Address }
            .firstOrNull(::isUsableNonLoopbackIpv4)
            ?.hostAddress
    }

    private fun getInterfaceCandidates(): List<InterfaceCandidate> {
        return try {
            NetworkInterface.getNetworkInterfaces()
                .toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .filterNot { networkInterface ->
                    ignoredInterfacePrefixes.any { prefix ->
                        networkInterface.name.startsWith(prefix, ignoreCase = true)
                    }
                }
                .flatMap { networkInterface ->
                    networkInterface.inetAddresses.toList().asSequence().mapNotNull { inetAddress ->
                        val ipv4 = inetAddress as? Inet4Address ?: return@mapNotNull null
                        val host = ipv4.hostAddress ?: return@mapNotNull null
                        if (!isUsableNonLoopbackIpv4(ipv4)) {
                            return@mapNotNull null
                        }

                        InterfaceCandidate(
                            host = host,
                            interfaceName = networkInterface.name,
                            isHotspot = hotspotInterfacePrefixes.any { prefix ->
                                networkInterface.name.startsWith(prefix, ignoreCase = true)
                            }
                        )
                    }
                }
                .distinctBy { it.host }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isUsableNonLoopbackIpv4(address: Inet4Address): Boolean {
        if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isMulticastAddress) {
            return false
        }

        val host = address.hostAddress ?: return false
        val octets = host.split(".").mapNotNull { it.toIntOrNull() }
        if (octets.size != 4) return false

        return octets[0] != 0 && octets[0] != 255
    }

    private fun readHotspotEnabledState(context: Context): Boolean? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null

        runCatching {
            val method = WifiManager::class.java.getMethod("isWifiApEnabled")
            method.invoke(wifiManager) as? Boolean
        }.getOrNull()?.let { return it }

        return runCatching {
            val method = WifiManager::class.java.getMethod("getWifiApState")
            val state = method.invoke(wifiManager) as? Int ?: return@runCatching null
            state == WIFI_AP_STATE_ENABLED_FALLBACK
        }.getOrNull()
    }

    private fun readWifiSsid(context: Context): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ssid = wifiManager.connectionInfo?.ssid?.removeSurrounding("\"").orEmpty()
            when {
                ssid.isBlank() -> "Wi-Fi"
                ssid == "<unknown ssid>" || ssid == "0x" -> "Wi-Fi"
                else -> ssid
            }
        } catch (_: SecurityException) {
            "Wi-Fi"
        } catch (_: Exception) {
            "Wi-Fi"
        }
    }

    private data class InterfaceCandidate(
        val host: String,
        val interfaceName: String,
        val isHotspot: Boolean
    )
}
