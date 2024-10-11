package com.example.privatevpn

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
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
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
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
    private lateinit var vpnProfile: VpnProfile
    private lateinit var countryNameTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var uploadSpeedTextView: TextView
    private lateinit var downloadSpeedTextView: TextView
    private var isAnimating = false
    private var isConnected = false

    // App Open Ad
    private var appOpenAd: AppOpenAd? = null
    private var isAdShowing = false

    // Banner Ad
    private lateinit var adView: AdView

    // Data from API
    private var serverData: String? = null

    // Variables for tracking internet speed
    private var previousRxBytes = 0L
    private var previousTxBytes = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val speedCheckInterval: Long = 1000  // 1-second interval for checking speed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Load and show App Open Ad
        loadAndShowAppOpenAd()

        // Initialize Views
        initializeViews()

        // Load Banner Ad
        loadBannerAd()

        // Check for Internet Connectivity
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show()
        }

        // Set up notification permission check for Android 13+
        requestNotificationPermission()

        // Button listeners
        setButtonListeners()

        // Fetch server data from API at the start
        fetchServersData()

        // Start monitoring network speed
        startNetworkSpeedMonitor()
    }

    private fun fetchServersData() {
        // Configure OkHttpClient with connection pooling, keep-alive, and timeouts
        val client = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)  // Set connection timeout
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)     // Set read timeout
            .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)    // Set write timeout
            .retryOnConnectionFailure(true)                             // Retry on failure
            .connectionPool(ConnectionPool(5, 5, java.util.concurrent.TimeUnit.MINUTES)) // Connection pool for faster reuse
            .addInterceptor { chain ->
                // Add GZIP compression for faster response
                val request = chain.request().newBuilder()
                    .addHeader("Accept-Encoding", "gzip")
                    .build()
                chain.proceed(request)
            }
            .build()

        val request = Request.Builder()
            .url("https://govpn.ai/api/servers/")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to load server data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val data = responseBody.string()

                    // Store the server data
                    serverData = data
                }
            }
        })
    }

    private fun loadAndShowAppOpenAd() {
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            this,
            "ca-app-pub-1156738411664537/8098654971", // Your Ad Unit ID
            adRequest,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    showAdIfAvailable()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e("AdMob", "Failed to load app open ad: ${loadAdError.message}")
                    Handler(Looper.getMainLooper()).postDelayed({
                        loadAndShowAppOpenAd() // Retry loading the ad after failure
                    }, 3000) // Retry after 3 seconds
                }
            }
        )
    }

    private fun showAdIfAvailable() {
        if (appOpenAd != null && !isAdShowing) {
            appOpenAd?.show(this)
            isAdShowing = true
        }
    }

    private fun loadBannerAd() {
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d("AdMob", "Banner ad loaded successfully")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e("AdMob", "Failed to load banner ad: ${error.message}")
                Handler(Looper.getMainLooper()).postDelayed({
                    adView.loadAd(adRequest)
                }, 5000)
            }
        }
    }

    override fun onPause() {
        if (::adView.isInitialized) {
            adView.pause()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::adView.isInitialized) {
            adView.resume()
        }
    }

    override fun onDestroy() {
        if (::adView.isInitialized) {
            adView.destroy()
        }
        super.onDestroy()
    }

    private fun initializeViews() {
        countrySelector = findViewById(R.id.countrySelector)
        premium = findViewById(R.id.premium)
        toggle = findViewById(R.id.toggle)
        button = findViewById(R.id.button)
        statusTextView = findViewById(R.id.textView)
        countryNameTextView = findViewById(R.id.countryName)
        profileImageView = findViewById(R.id.profile_image)
        uploadSpeedTextView = findViewById(R.id.upload_speed)
        downloadSpeedTextView = findViewById(R.id.download_speed)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    102
                )
            }
        }
    }

    private fun setButtonListeners() {
        toggle.setOnClickListener {
            startActivity(Intent(this, sidenavigationActivity::class.java))
        }

        premium.setOnClickListener {
            startActivity(Intent(this, subciActivity::class.java))
        }

        countrySelector.setOnClickListener {
            if (serverData != null) {
                // Start AllServersActivity and pass the server data
                val intent = Intent(this, AllServersActivity::class.java)
                intent.putExtra("SERVER_DATA", serverData) // Pass server data
                startActivity(intent)
            } else {
                Toast.makeText(this, "Server data not yet loaded", Toast.LENGTH_SHORT).show()
            }
        }

        button.setOnClickListener {
            if (isAnimating) return@setOnClickListener

            isAnimating = true
            button.loop(true)
            button.playAnimation()

            val serverId = intent.getIntExtra("SERVER_ID", 1)
            fetchOpenVPNProfile(serverId)
            statusTextView.text = "Connecting..."

            Handler(Looper.getMainLooper()).postDelayed({
                button.cancelAnimation()
                button.setProgress(1f)
                statusTextView.text = "Tap again to Connect."
                isAnimating = false
            }, 5000)
        }

        loadSelectedLanguage()
        val countryName = intent.getStringExtra("COUNTRY_NAME") ?: "United States"
        val flagUrl = intent.getStringExtra("FLAG_URL") ?: ""
        updateCountrySelector(countryName, flagUrl)
        checkVPNConnection()
    }

    private fun checkVPNConnection() {
        val vpnServiceIntent = Intent(this, de.blinkt.openvpn.core.OpenVPNService::class.java)
        if (isServiceRunning(vpnServiceIntent)) {
            val intent = Intent(this, ConnectActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isServiceRunning(serviceIntent: Intent): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (service.service.className == serviceIntent.component?.className) {
                return true
            }
        }
        return false
    }

    fun updateCountrySelector(countryName: String, flagUrl: String) {
        countryNameTextView.text = countryName
        if (flagUrl.isNotEmpty()) {
            Glide.with(this)
                .load(flagUrl)
                .into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.us)
        }
    }

    private fun fetchOpenVPNProfile(serverId: Int) {
        val client = OkHttpClient()
        val requestUrl = "https://govpn.ai/api/ovpn-file/$serverId?vpn_username=12312"
        val request = Request.Builder().url(requestUrl).get().build()

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
                        runOnUiThread { parseAndStartVPN(responseBody) }
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

                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, ConnectActivity::class.java)
                    startActivity(intent)
                    finish()
                }, 3000)
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Close", Toast.LENGTH_SHORT).show()
        super.onBackPressed()
    }

    // Function to start monitoring internet speed
    private fun startNetworkSpeedMonitor() {
        previousRxBytes = TrafficStats.getTotalRxBytes()
        previousTxBytes = TrafficStats.getTotalTxBytes()

        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()

                // Calculate download and upload speeds in KBps
                val downloadSpeed = (currentRxBytes - previousRxBytes) / 1024  // KBps
                val uploadSpeed = (currentTxBytes - previousTxBytes) / 1024  // KBps

                // Update the UI with the current speeds
                downloadSpeedTextView.text = "$downloadSpeed KB/s"
                uploadSpeedTextView.text = "$uploadSpeed KB/s"

                // Update previous values for the next calculation
                previousRxBytes = currentRxBytes
                previousTxBytes = currentTxBytes

                // Schedule the next speed check
                handler.postDelayed(this, speedCheckInterval)
            }
        }, speedCheckInterval)
    }
}
