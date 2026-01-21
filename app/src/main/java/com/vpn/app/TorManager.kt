package com.vpn.app

import android.content.Context
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import java.util.Timer

class TorManager(private val context: Context) {
    fun startTor() {
        val proxyManager = AndroidOnionProxyManager(context, "torfiles")
        if (!proxyManager.isRunning) {
            try {
                // Start Tor on port 9050
                // This starts the local SOCKS proxy
                proxyManager.startWithRepeat(9050, 60, Timer())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}