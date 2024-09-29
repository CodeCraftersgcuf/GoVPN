package com.example.privatevpn

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService // Import Android's VpnService
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

lateinit var countrySelector: LinearLayout
lateinit var premium: ImageView
lateinit var toggle: ImageView
lateinit var button: ImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        countrySelector = findViewById(R.id.countrySelector)
        premium = findViewById(R.id.priemum)
        toggle = findViewById(R.id.toggle)
        button = findViewById(R.id.button)

        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 102)
            }
        }

        // Set up click listeners
        toggle.setOnClickListener {
            val intent = Intent(this, sidenavigationActivity::class.java)
            startActivity(intent)
        }

        premium.setOnClickListener {
            val intent = Intent(this, subciActivity::class.java)
            startActivity(intent)
        }

        countrySelector.setOnClickListener {
            val intent = Intent(this, AllServersActivity::class.java)
            startActivity(intent)
        }

        button.setOnClickListener {
            importOpenVPNProfile() // Import and start VPN connection
        }

        // Load the selected language on app start
        loadSelectedLanguage()
    }

    private fun importOpenVPNProfile() {
        try {
            // Read the .ovpn file from assets
            val inputStream = assets.open("new.ovpn")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val config = bufferedReader.readText()

            // Parse the configuration file using ConfigParser
            val cp = ConfigParser()
            cp.parseConfig(InputStreamReader(assets.open("new.ovpn")))

            // Get the VPN profile from the parser
            val vpnProfile = cp.convertProfile()

            // Save profile to ProfileManager
            ProfileManager.getInstance(this).addProfile(vpnProfile)

            // Connect to the VPN
            startVPN(vpnProfile)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to import OpenVPN profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVPN(vpnProfile: VpnProfile) {
        try {
            // Prepare VPN service
            val intent = VpnService.prepare(this) // Use Android's VpnService
            if (intent != null) {
                startActivityForResult(intent, 0)
            } else {
                VPNLaunchHelper.startOpenVpn(vpnProfile, this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start VPN", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSelectedLanguage() {
        val sharedPreferences = getSharedPreferences("app_language", MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("selected_language", "en") // Default to English
        setLocale(selectedLanguage ?: "en")
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Close", Toast.LENGTH_SHORT).show()
        super.onBackPressed()
    }
}
