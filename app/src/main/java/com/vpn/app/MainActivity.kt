package com.vpn.app

import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    // File Picker for .ovpn files
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveConfigToInternalStorage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val btnImport = findViewById<Button>(R.id.btnImport)
        val btnStart = findViewById<Button>(R.id.btnStart)

        // Start Tor Engine immediately in background
        Thread { TorManager(this).startTor() }.start()
        updateStatus("Tor Starting...")

        // Button: Import Config
        btnImport.setOnClickListener {
            // Launches file picker
            filePickerLauncher.launch("*/*")
        }

        // Button: Start VPN
        btnStart.setOnClickListener {
            if (isConfigExists()) {
                prepareVpn()
            } else {
                Toast.makeText(this, "Please Import an OpenVPN Config first!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveConfigToInternalStorage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader().use { it?.readText() }
            
            if (content != null) {
                // Save to private app storage as "user_config.ovpn"
                val file = File(filesDir, "user_config.ovpn")
                file.writeText(content)
                updateStatus("Config Loaded: ${file.name}")
                Toast.makeText(this, "Configuration Saved Successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isConfigExists(): Boolean {
        return File(filesDir, "user_config.ovpn").exists()
    }

    private fun updateStatus(msg: String) {
        runOnUiThread { statusText.text = msg }
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            startService(Intent(this, TorVpnService::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            startService(Intent(this, TorVpnService::class.java))
        }
    }
}