package com.example.privatevpn

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.os.CountDownTimer
import androidx.appcompat.app.AlertDialog
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import android.provider.Settings
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.google.android.gms.ads.*


lateinit var profile_image: CircleImageView
lateinit var priemums: ImageView
lateinit var toggles: ImageView
lateinit var disconnect: Button
lateinit var time: TextView
lateinit var ipAddress: TextView
lateinit var materialButton: Button
lateinit var uploadSpeedTextView: TextView
lateinit var downloadSpeedTextView: TextView

class ConnectActivity : AppCompatActivity() {
    private var startTime: Long = 0 // Variable to store connection start time
    private var countryName: String = "" // To store the country name
    private lateinit var timer: CountDownTimer
    private val thirtyMinutesInMillis: Long = 30 * 60 * 1000  // 30 minutes in milliseconds
    private var timeLeftInMillis: Long = thirtyMinutesInMillis
    private lateinit var sharedPreferences: SharedPreferences
    private var isTimerRunning = false
    private val additionalTimeInMillis: Long = 60 * 60 * 1000  // 60 minutes in milliseconds
    private var isVpnDisconnected = false  // Flag to prevent multiple redirects

    // Handler for periodically checking IP and network speed
    private val handler = Handler(Looper.getMainLooper())
    private val ipCheckInterval: Long = 1000  // Check IP status every 1 second

    // Handler for secondary IP check logic with 3-second delay
    private val secondaryHandler = Handler(Looper.getMainLooper())
    private val secondaryIpCheckInterval: Long = 3000  // Check IP status every 3 seconds

    // Variables for network speed monitoring
    private var previousRxBytes = 0L
    private var previousTxBytes = 0L
    private val speedCheckInterval: Long = 1000  // 1-second interval for checking speed
    // App Open Ad
    private var appOpenAd: AppOpenAd? = null
    private var isAdShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        // Initialize views
        profile_image = findViewById(R.id.profile_image)
        priemums = findViewById(R.id.priemums)
        toggles = findViewById(R.id.toggles)
        disconnect = findViewById(R.id.disconnect)
        time = findViewById(R.id.time)
        ipAddress = findViewById(R.id.ipAddress)
        materialButton = findViewById(R.id.materialButton)
        uploadSpeedTextView = findViewById(R.id.upload_speed)
        downloadSpeedTextView = findViewById(R.id.download_speed)

        // Initialize sharedPreferences before use
        sharedPreferences = getSharedPreferences("vpnPrefs", MODE_PRIVATE)

        // Check subscription status and set timer accordingly
        checkSubscriptionStatus()

        // Get saved start time from sharedPreferences, or set it to the current time if not present
        val savedStartTime = sharedPreferences.getLong("vpnStartTime", 0L)
        startTime = if (savedStartTime != 0L) savedStartTime else System.currentTimeMillis()

        // Save the start time for the current VPN session if it hasn't been saved yet
        if (savedStartTime == 0L) {
            sharedPreferences.edit().putLong("vpnStartTime", startTime).apply()
        }

        // Get saved time and timer state
        timeLeftInMillis = sharedPreferences.getLong("timeLeftInMillis", thirtyMinutesInMillis)
        isTimerRunning = sharedPreferences.getBoolean("isTimerRunning", false)

        // Display the time when the activity is created
        updateTimerText()

        // Start the timer if it is not running and has time left
        if (isTimerRunning && timeLeftInMillis > 0) {
            startTimer()
        } else if (!isTimerRunning && timeLeftInMillis > 0) {
            updateTimerText()  // Display the saved time
        }

        // Set onClickListeners for the buttons
        setOnClickListeners()

        // Start checking for IP changes every 1 second
        handler.postDelayed(ipStatusChecker, ipCheckInterval)

        // Start secondary IP check logic every 3 seconds
        secondaryHandler.postDelayed(secondaryIpStatusChecker, secondaryIpCheckInterval)

        // Start monitoring network speed every second
        startNetworkSpeedMonitor()
    }


    override fun onResume() {
        super.onResume()

        // Fetch the IP immediately when returning to the app
        fetchIpAddress()

        // Restart the timer if the app returns to this activity after disconnection
        if (!isTimerRunning && timeLeftInMillis > 0) {
            startTimer()
        }
    }

    private fun setOnClickListeners() {
        profile_image.setOnClickListener {
            Toast.makeText(this, "Please disconnect VPN before Changing Server", Toast.LENGTH_SHORT).show()
        }

        priemums.setOnClickListener {
            val intent = Intent(this, subciActivity::class.java)
            startActivity(intent)
        }

        toggles.setOnClickListener {
            val intent = Intent(this, sidenavigationActivity::class.java)
            startActivity(intent)
        }

        disconnect.setOnClickListener {
            showDisconnectDialog()
        }

        // Add 60 minutes to the timer when the button is clicked
        materialButton.setOnClickListener {
            increaseTimerBy60Minutes()
        }
    }

    private fun updateTimerText() {
        val seconds = (timeLeftInMillis / 1000) % 60
        val minutes = (timeLeftInMillis / (1000 * 60)) % 60
        val hours = (timeLeftInMillis / (1000 * 60 * 60)) % 24
        time.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun startTimer() {
        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000) % 60
                val currentSeconds = (timeLeftInMillis / 1000) % 60

                if (secondsRemaining != currentSeconds) {
                    timeLeftInMillis = millisUntilFinished
                    updateTimerText()
                }

                // Save the remaining time to SharedPreferences every second
                val editor = sharedPreferences.edit()
                editor.putLong("timeLeftInMillis", timeLeftInMillis)
                editor.putBoolean("isTimerRunning", true)
                editor.apply()
            }

            override fun onFinish() {
                Toast.makeText(this@ConnectActivity, "VPN session expired, adding 1 minute.", Toast.LENGTH_SHORT).show()

                // Add 1 minute (60 * 1000 milliseconds)
                timeLeftInMillis += 60 * 1000

                // Restart the timer with the updated time
                if (isTimerRunning) {
                    timer.cancel()  // Cancel the current timer
                }

                // Update the UI with the new time and restart the timer
                updateTimerText()
                startTimer()
            }

        }.start()
        isTimerRunning = true
    }

    private fun stopTimer() {
        if (::timer.isInitialized) {
            timer.cancel()
        }
        isTimerRunning = false

        // Save the state when the timer is stopped
        val editor = sharedPreferences.edit()
        editor.putBoolean("isTimerRunning", false)
        editor.putLong("timeLeftInMillis", timeLeftInMillis)
        editor.apply()
    }

    private fun increaseTimerBy60Minutes() {
        if (timeLeftInMillis >= 60 * 60 * 1000) {
            Toast.makeText(this, "Cannot add more time, already over 1 hour", Toast.LENGTH_SHORT).show()
            return
        }

        // Show the countdown dialog
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.countdown_dialog)
        dialog.setCancelable(false)  // Prevent user from closing it early

        // Get references to the TextViews in the dialog
        val timerTextView = dialog.findViewById<TextView>(R.id.timerCountdown)
        val countdownText = dialog.findViewById<TextView>(R.id.countdownTimerText)

        // Show the dialog
        dialog.show()

        // Start the countdown from 7 seconds
        val countdownTime = 7
        var timeRemaining = countdownTime

        // Create a Handler to update the countdown every second
        val countdownHandler = Handler(Looper.getMainLooper())
        val countdownRunnable = object : Runnable {
            override fun run() {
                if (timeRemaining > 0) {
                    // Update the timer in the dialog
                    timerTextView.text = timeRemaining.toString()
                    timeRemaining--

                    // Schedule the next update in 1 second
                    countdownHandler.postDelayed(this, 1000)
                } else {
                    // When countdown is finished, dismiss the dialog and proceed
                    dialog.dismiss()

                    // Load and show the ad
                    loadAndShowAppOpenAd()

                    // After the ad is shown, introduce a 5-second delay before adding time
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Add 60 minutes to the current time left
                        timeLeftInMillis += additionalTimeInMillis

                        if (isTimerRunning) {
                            timer.cancel()
                            startTimer()
                        } else {
                            updateTimerText()
                            startTimer()
                        }

                        Toast.makeText(this@ConnectActivity, "Added 60 minutes to the session", Toast.LENGTH_SHORT).show()
                    }, 10)
                }
            }
        }

        // Start the countdown
        countdownHandler.post(countdownRunnable)
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
    private fun checkSubscriptionStatus() {
        // Get device ID (Android ID)
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // Prepare the request body
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            JSONObject().put("deviceId", deviceId).toString()
        )

        // Make a POST request to check subscription status
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://govpn.ai/api/checksubscription")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ConnectActivity, "Failed to check subscription status", Toast.LENGTH_SHORT).show()
                    // Default to 30-minute timer in case of failure
                    timeLeftInMillis = thirtyMinutesInMillis
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseJson = JSONObject(responseBody.string())
                    val isPremium = responseJson.getBoolean("status")

                    // Set the timer based on subscription status
                    runOnUiThread {
                        if (isPremium) {
                            // Set timer to 24 hours for premium users
                            timeLeftInMillis = 24 * 60 * 60 * 1000  // 24 hours in milliseconds

                            // Check if the timer is running, cancel it and restart with the new time
                            if (isTimerRunning) {
                                timer.cancel()  // Cancel the current running timer
                                startTimer()     // Start a new timer with updated time
                            } else {
                                updateTimerText()  // Update the UI to reflect the new time
                                startTimer()       // Start the timer
                            }

                            Toast.makeText(this@ConnectActivity, "Premium user: 24 hours timer", Toast.LENGTH_SHORT).show()
                        } else {
//                            timeLeftInMillis = thirtyMinutesInMillis // 30 minutes for regular users
//                            // Set timer to 30 minutes for regular users
//                            timeLeftInMillis = thirtyMinutesInMillis
//
//                            // Check if the timer is running, cancel it and restart with the new time
//                            if (isTimerRunning) {
//                                timer.cancel()  // Cancel the current running timer
//                                startTimer()     // Start a new timer with updated time
//                            } else {
//                                updateTimerText()  // Update the UI to reflect the new time
//                                startTimer()       // Start the timer
//                            }
//                            Toast.makeText(this@ConnectActivity, "Regular user: 30 minutes timer", Toast.LENGTH_SHORT).show()
                        }

                    }
                }
            }
        })
    }

    // Start monitoring network speed (upload and download)
    private fun startNetworkSpeedMonitor() {
        previousRxBytes = TrafficStats.getTotalRxBytes()
        previousTxBytes = TrafficStats.getTotalTxBytes()

        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()

                // Calculate the difference to get the speed in bytes/second
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

    private fun fetchIpAddress() {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        Handler(Looper.getMainLooper()).postDelayed({
            val request = Request.Builder()
                .url("https://api64.ipify.org?format=json") // Alternative API to get IP address
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        ipAddress.text = "Unknown IP (Failure)"
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val responseData = response.body?.string()
                        println("Response code: ${response.code}")
                        println("Response body: $responseData")

                        runOnUiThread {
                            if (response.isSuccessful && responseData != null) {
                                try {
                                    val jsonObject = JSONObject(responseData)
                                    val ip = jsonObject.getString("ip")
                                    ipAddress.text = "IP: $ip"

                                    // Fetch the country information based on the IP
                                    fetchCountryInfo(ip)

                                    val storedIp = sharedPreferences.getString("storedIp", "0.0.0.0")
                                    if (storedIp == "0.0.0.0") {
                                        sharedPreferences.edit()
                                            .putString("storedIp", ip)
                                            .apply()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    ipAddress.text = "Unknown IP (Exception)"
                                }
                            } else {
                                ipAddress.text = "Unknown IP (Unsuccessful Response)"
                            }
                        }
                    }
                }
            })
        }, 2000)
    }

    private fun fetchCountryInfo(ip: String) {
        countryName = when (ip) {
            "157.245.83.117" -> "United States"
            "139.59.171.30" -> "United Kingdom"
            else -> "Unknown"
        }
    }

    private val ipStatusChecker = object : Runnable {
        override fun run() {
            fetchAndCheckIpAddress()
            if (!isVpnDisconnected) {
                // Adding a 2-second delay
                handler.postDelayed(this, ipCheckInterval + 5000)  // Delay of 2 seconds added
            }
        }
    }

    private fun fetchAndCheckIpAddress() {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url("https://ipinfo.io/json").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure silently
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                response.body?.close()
                runOnUiThread {
                    if (response.isSuccessful && responseData != null) {
                        try {
                            val jsonObject = JSONObject(responseData)
                            val currentIp = jsonObject.getString("ip")
                            checkIpAddress(currentIp)
                        } catch (e: Exception) {
                            // Handle failure silently
                        }
                    }
                }
            }
        })
    }

    private val secondaryIpStatusChecker = object : Runnable {
        override fun run() {
            performSecondaryIpCheck()
            if (!isVpnDisconnected) {
                secondaryHandler.postDelayed(this, secondaryIpCheckInterval)
            }
        }
    }

    private fun checkIpAddress(currentIp: String) {
        val allowedIps = setOf("139.59.171.30", "157.245.83.117", "0.0.0.0")
        val storedIp = sharedPreferences.getString("storedIp", "0.0.0.0") ?: "0.0.0.0"

        if (currentIp !in allowedIps || storedIp !in allowedIps) {
            stopTimer()
            if (!isVpnDisconnected) {
                isVpnDisconnected = true
                Toast.makeText(this, "VPN disconnected due to IP change.", Toast.LENGTH_SHORT).show()
                val serverId = intent.getIntExtra("SERVER_ID", 1)
                disconnectVPN(serverId)
            }
        }
    }

    private fun performSecondaryIpCheck() {
        fetchIpAddress()
    }

    private fun showDisconnectDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_disconnect, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)

        val countdownTimeInMillis: Long = 5000
        val countDownTimer = object : CountDownTimer(countdownTimeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                confirmButton.text = String.format("00:%02d", secondsRemaining)
            }

            override fun onFinish() {
                confirmButton.text = "Disconnect"
                confirmButton.isEnabled = true
            }
        }
        countDownTimer.start()
        confirmButton.isEnabled = false

        confirmButton.setOnClickListener {
            if (confirmButton.text == "Disconnect") {
                val serverId = intent.getIntExtra("SERVER_ID", 1)
                disconnectVPN(serverId)
                stopTimer()
                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener {
            countDownTimer.cancel()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun disconnectVPN(serverId: Int) {
        try {
            // Stop any running timers and handlers
            stopTimer()
            handler.removeCallbacksAndMessages(null)
            secondaryHandler.removeCallbacksAndMessages(null)

            // Send an intent to disconnect the VPN using the OpenVPNService
            val disconnectIntent = Intent(this, de.blinkt.openvpn.core.OpenVPNService::class.java)
            disconnectIntent.action = de.blinkt.openvpn.core.OpenVPNService.DISCONNECT_VPN
            startService(disconnectIntent)

            // Get the unique phone ID (ANDROID_ID) as the vpn_username
            val vpnUsername = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

            // Send a POST request to notify the server about the disconnection
            val client = OkHttpClient()
            val requestUrl = "https://govpn.ai/api/disconnect"

            // Create a request body with server_id and vpn_username
            val requestBody = FormBody.Builder()
                .add("server_id", serverId.toString())
                .add("vpn_username", vpnUsername)
                .build()

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("DisconnectVPN", "Failed to send disconnect notification", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        if (res.isSuccessful) {
                            Log.d("DisconnectVPN", "Disconnect notification successful")
                        } else {
                            Log.e("DisconnectVPN", "Failed to notify: ${res.code}, ${res.message}")
                        }
                    }
                }
            })

            // Add a short delay to ensure VPN disconnection is processed before moving to DisconnectActivity
            Handler(Looper.getMainLooper()).postDelayed({
                // Get start time from SharedPreferences
                val startTime = sharedPreferences.getLong("vpnStartTime", 0L)
                if (startTime == 0L) {
                    // Handle the case where start time wasn't properly recorded
                    Toast.makeText(this, "Error calculating VPN duration", Toast.LENGTH_SHORT).show()
                    return@postDelayed
                }

                // Calculate connection duration
                val currentTime = System.currentTimeMillis()
                val durationMillis = currentTime - startTime

                // Reset the start time for future sessions
                sharedPreferences.edit().putLong("vpnStartTime", 0L).apply()

                // Prepare to redirect to DisconnectActivity
                val intent = Intent(this, DisconnectActivity::class.java)
                intent.putExtra("IP_ADDRESS", ipAddress.text.toString().substringAfter(": "))
                intent.putExtra("DURATION_MILLIS", durationMillis) // Pass duration in milliseconds
                intent.putExtra("COUNTRY_NAME", countryName)
                startActivity(intent)

                finish() // Finish ConnectActivity if you don't want to return to it
            }, 1000) // 1-second delay to ensure VPN disconnection is processed

        } catch (e: Exception) {
            // Handle failure silently
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        secondaryHandler.removeCallbacksAndMessages(null)
    }
}
