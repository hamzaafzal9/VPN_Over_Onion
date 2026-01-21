package com.vpn.app

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.File
import java.nio.charset.Charset

class TorVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        // 1. Read the OpenVPN Config
        val configFile = File(filesDir, "user_config.ovpn")
        if (!configFile.exists()) {
            println("ERROR: No .ovpn config file found!")
            return START_NOT_STICKY
        }

        val ovpnConfig = configFile.readText(Charset.defaultCharset())

        // 2. CHECK: Is this config TCP? (Critical for Tor)
        if (!ovpnConfig.contains("proto tcp")) {
            println("WARNING: This config uses UDP! It might fail over Tor. Please use 'proto tcp'.")
        }

        // 3. Configure the Local Interface
        val builder = Builder()
        builder.setSession("TorVPN (OpenVPN)")
        builder.setMtu(1500)
        builder.addAddress("10.8.0.2", 24) // Standard OpenVPN internal IP
        builder.addRoute("0.0.0.0", 0)

        // 4. CRITICAL: Split Tunneling
        // We MUST exclude our own app so Tor traffic (and the VPN connection traffic)
        // doesn't loop back into the VPN interface.
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        vpnInterface = builder.establish()
        println("VPN Interface Started. Ready for OpenVPN Engine.")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnInterface?.close()
    }
}