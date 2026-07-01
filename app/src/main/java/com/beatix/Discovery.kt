package com.beatix

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

/**
 * Finds the Mac's Beatix bridge on the local Wi-Fi via mDNS/Bonjour
 * (service type "_beatix._tcp"), so the user never types an IP.
 */
class NsdDiscovery(context: Context) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.DiscoveryListener? = null
    private val type = "_beatix._tcp."

    fun start(onFound: (String) -> Unit) {
        stop()
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(s: String) {}
            override fun onDiscoveryStopped(s: String) {}
            override fun onStartDiscoveryFailed(s: String, code: Int) {}
            override fun onStopDiscoveryFailed(s: String, code: Int) {}
            override fun onServiceLost(info: NsdServiceInfo) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                if (!info.serviceType.contains("beatix")) return
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(s: NsdServiceInfo, code: Int) {}
                    override fun onServiceResolved(s: NsdServiceInfo) {
                        s.host?.hostAddress?.let(onFound)
                    }
                })
            }
        }
        listener = l
        try {
            nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, l)
        } catch (_: Exception) {
        }
    }

    fun stop() {
        listener?.let { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }
        listener = null
    }
}
