package com.example.privatevpn

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import okhttp3.*
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var countrySelector: LinearLayout
    private lateinit var premium: ImageView
    private lateinit var toggle: ImageView
    private lateinit var button: LottieAnimationView
    private lateinit var statusTextView: TextView

    private var isConnected = false
    private lateinit var vpnProfile: VpnProfile
    private lateinit var countryNameTextView: TextView
    private lateinit var profileImageView: ImageView

    private var isAnimating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        countrySelector = findViewById(R.id.countrySelector)
        premium = findViewById(R.id.premium)
        toggle = findViewById(R.id.toggle)
        button = findViewById(R.id.button)
        statusTextView = findViewById(R.id.textView)
        countryNameTextView = findViewById(R.id.countryName)
        profileImageView = findViewById(R.id.profile_image)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 102)
            }
        }

        toggle.setOnClickListener {
            startActivity(Intent(this, sidenavigationActivity::class.java))
        }

        premium.setOnClickListener {
            startActivity(Intent(this, subciActivity::class.java))
        }

        countrySelector.setOnClickListener {
            startActivity(Intent(this, AllServersActivity::class.java))
        }

        button.setOnClickListener {
            if (isAnimating) return@setOnClickListener

            isAnimating = true
            button.loop(true)
            button.playAnimation()

            if (isConnected) {
                disconnectVPN()
                statusTextView.text = "Disconnecting..."

                Handler(Looper.getMainLooper()).postDelayed({
                    button.cancelAnimation()
                    button.setProgress(1f)
                    statusTextView.text = "Tap to Connect"
                    isAnimating = false
                }, 1000)

            } else {
                val serverId = intent.getIntExtra("SERVER_ID", -1)
                if (serverId != -1) {
                    fetchOpenVPNProfile(serverId)
                    statusTextView.text = "Connecting..."

                    Handler(Looper.getMainLooper()).postDelayed({
                        button.cancelAnimation()
                        button.setProgress(1f)
                        statusTextView.text = "Tap to Disconnect"
                        isAnimating = false
                    }, 4000)
                } else {
                    Toast.makeText(this, "Invalid Server ID", Toast.LENGTH_SHORT).show()
                    isAnimating = false
                }
            }
        }

        loadSelectedLanguage()

        // Retrieve country data from Intent
        val countryName = intent.getStringExtra("COUNTRY_NAME") ?: "United States"
        val flagUrl = intent.getStringExtra("FLAG_URL") ?: "" // Assuming flagUrl is passed correctly

        updateCountrySelector(countryName, flagUrl)
    }

    fun updateCountrySelector(countryName: String, flagUrl: String) {
        countryNameTextView.text = countryName
        if (flagUrl.isNotEmpty()) {
            Glide.with(this)
                .load(flagUrl)
                .into(profileImageView)
        } else {
            // Set a default image or handle error case
            profileImageView.setImageResource(R.drawable.us) // Replace with your default flag resource
        }
    }

    private fun fetchOpenVPNProfile(serverId: Int) {
        val client = OkHttpClient()
        val requestUrl = "https://govpn.ai/api/ovpn-file/$serverId?vpn_username=12312"
        val request = Request.Builder()
            .url(requestUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to fetch VPN profile", Toast.LENGTH_SHORT).show()
                    Log.e("VPNResponse", "Failed to fetch VPN profile", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val responseBody = res.body?.string()
                    if (res.isSuccessful && responseBody != null) {
                        Log.d("VPNResponse", "Success: $responseBody")
                        runOnUiThread {
                            parseAndStartVPN(responseBody)
                        }
                    } else {
                        Log.e("VPNResponse", "Failed: ${res.code}, ${res.message}")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Failed to fetch VPN profile: ${res.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun parseAndStartVPN(ovpnConfig: String) {
        try {
            val cp = ConfigParser()
            cp.parseConfig(InputStreamReader(ovpnConfig.byteInputStream()))
            vpnProfile = cp.convertProfile()
            ProfileManager.getInstance(this).addProfile(vpnProfile)
            startVPN(vpnProfile)
        } catch (e: Exception) {
            Log.e("VPN", "Error parsing VPN profile", e)
            runOnUiThread {
                Toast.makeText(this, "Failed to import OpenVPN profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVPN(vpnProfile: VpnProfile) {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, 0)
        } else {
            try {
                ProfileManager.getInstance(this).addProfile(vpnProfile)
                VPNLaunchHelper.startOpenVpn(vpnProfile, this)
                isConnected = true
            } catch (e: Exception) {
                Log.e("VPN", "Failed to start VPN", e)
                Toast.makeText(this, "Failed to start VPN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disconnectVPN() {
        try {
            val disconnectIntent = Intent(this, de.blinkt.openvpn.core.OpenVPNService::class.java)
            disconnectIntent.action = de.blinkt.openvpn.core.OpenVPNService.DISCONNECT_VPN
            startService(disconnectIntent)
            isConnected = false
            Toast.makeText(this, "Disconnected successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("VPN", "Failed to disconnect VPN", e)
            Toast.makeText(this, "Failed to disconnect VPN", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSelectedLanguage() {
        val sharedPreferences = getSharedPreferences("app_language", MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("selected_language", "en")
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
