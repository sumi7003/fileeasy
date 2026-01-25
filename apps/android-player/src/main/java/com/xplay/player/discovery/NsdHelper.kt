package com.xplay.player.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NsdHelper(context: Context) {
    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null
    
    private var retryJob: Job? = null
    private var retryCount = 0
    private val MAX_RETRY = 5
    private val RETRY_DELAY_MS = 3000L

    fun registerHost(port: Int = 3000) {
        if (registrationListener != null) return
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD registered: ${serviceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD register failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD unregister failed: $errorCode")
            }
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun unregisterHost() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (_: Exception) {
            }
        }
        registrationListener = null
    }

    fun startDiscovery(onHostFound: (String) -> Unit) {
        if (discoveryListener != null) {
            Log.d(TAG, "Discovery already running")
            return
        }

        retryCount = 0
        startDiscoveryInternal(onHostFound)
    }
    
    private fun startDiscoveryInternal(onHostFound: (String) -> Unit) {
        // 申请多播锁，确保能收到 NSD 广播包
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("XplayNsdLock").apply {
                    setReferenceCounted(true)
                }
            }
            multicastLock?.acquire()
            Log.d(TAG, "Multicast lock acquired")
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire multicast lock: ${e.message}")
        }

        resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD resolve failed: $errorCode for ${serviceInfo.serviceName}")
                // 解析失败时可以尝试重新解析
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                    Log.d(TAG, "Resolve already active, will retry on next service found")
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val hostAddress = serviceInfo.host?.hostAddress
                if (!hostAddress.isNullOrBlank()) {
                    Log.i(TAG, "✅ NSD resolved host: $hostAddress (service: ${serviceInfo.serviceName})")
                    retryCount = 0  // 成功后重置重试计数
                    onHostFound(hostAddress)
                } else {
                    Log.w(TAG, "Service resolved but host address is null")
                }
            }
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.i(TAG, "✅ NSD discovery started successfully, searching for xplay services...")
                Log.d(TAG, "Listening for service type: $SERVICE_TYPE")
                retryCount = 0
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}, type: ${serviceInfo.serviceType}")
                if (serviceInfo.serviceType == SERVICE_TYPE &&
                    serviceInfo.serviceName.startsWith(SERVICE_NAME_PREFIX)
                ) {
                    Log.i(TAG, "🎯 Found Xplay service: ${serviceInfo.serviceName}, attempting to resolve...")
                    try {
                        nsdManager.resolveService(serviceInfo, resolveListener)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Failed to resolve service: ${e.message}")
                    }
                } else {
                    Log.d(TAG, "Ignoring non-xplay service: ${serviceInfo.serviceName}")
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.w(TAG, "⚠️ NSD service lost: ${serviceInfo.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "NSD discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "❌ NSD start discovery failed: errorCode=$errorCode")
                stopDiscovery()
                scheduleRetry(onHostFound)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "NSD stop discovery failed: errorCode=$errorCode")
                discoveryListener = null
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            Log.d(TAG, "NSD discovery initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting NSD discovery: ${e.message}", e)
            stopDiscovery()
            scheduleRetry(onHostFound)
        }
    }
    
    private fun scheduleRetry(onHostFound: (String) -> Unit) {
        if (retryCount >= MAX_RETRY) {
            Log.e(TAG, "Max NSD retry attempts reached ($MAX_RETRY), giving up")
            return
        }
        
        retryCount++
        Log.d(TAG, "Scheduling NSD retry $retryCount/$MAX_RETRY in ${RETRY_DELAY_MS}ms")
        
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(RETRY_DELAY_MS)
            Log.d(TAG, "Retrying NSD discovery (attempt $retryCount)")
            startDiscoveryInternal(onHostFound)
        }
    }

    fun stopDiscovery() {
        Log.d(TAG, "Stopping NSD discovery")
        
        // 释放多播锁
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.d(TAG, "Multicast lock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing multicast lock: ${e.message}")
        }
        
        retryJob?.cancel()
        retryJob = null
        retryCount = 0
        
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
                Log.d(TAG, "NSD discovery stopped successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping NSD discovery: ${e.message}")
            }
        }
        discoveryListener = null
        resolveListener = null
    }

    companion object {
        private const val TAG = "NsdHelper"
        private const val SERVICE_TYPE = "_xplay._tcp."
        // 使用一个固定的名称，有些系统可以将其解析为 xplay.local
        private const val SERVICE_NAME = "xplay" 
        private const val SERVICE_NAME_PREFIX = "xplay"
    }
}
