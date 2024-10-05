package com.example.privatevpn

import android.content.Intent
import android.content.SharedPreferences
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

lateinit var profile_image: CircleImageView
lateinit var priemums: ImageView
lateinit var toggles: ImageView
lateinit var disconnect: Button
lateinit var time: TextView
lateinit var ipAddress: TextView
lateinit var materialButton: Button

class ConnectActivity : AppCompatActivity() {

    private lateinit var timer: CountDownTimer
    private val thirtyMinutesInMillis: Long = 30 * 60 * 1000  // 30 minutes in milliseconds
    private var timeLeftInMillis: Long = thirtyMinutesInMillis
    private lateinit var sharedPreferences: SharedPreferences
    private var isTimerRunning = false
    private val additionalTimeInMillis: Long = 60 * 60 * 1000  // 60 minutes in milliseconds
    private var isVpnDisconnected = false  // Flag to prevent multiple redirects

    // Handler for periodically checking IP
    private val handler = Handler(Looper.getMainLooper())
    private val ipCheckInterval: Long = 1000  // Check IP status every 1 second

    // Handler for secondary IP check logic with 3-second delay
    private val secondaryHandler = Handler(Looper.getMainLooper())
    private val secondaryIpCheckInterval: Long = 3000  // Check IP status every 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        profile_image = findViewById(R.id.profile_image)
        priemums = findViewById(R.id.priemums)
        toggles = findViewById(R.id.toggles)
        disconnect = findViewById(R.id.disconnect)
        time = findViewById(R.id.time)
        ipAddress = findViewById(R.id.ipAddress)
        materialButton = findViewById(R.id.materialButton)

        sharedPreferences = getSharedPreferences("vpnPrefs", MODE_PRIVATE)

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
                Toast.makeText(this@ConnectActivity, "VPN session expired", Toast.LENGTH_SHORT).show()
                disconnectVPN()
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

        // Add 60 minutes to the current time left
        timeLeftInMillis += additionalTimeInMillis

        if (isTimerRunning) {
            timer.cancel()
            startTimer()
        } else {
            updateTimerText()
            startTimer()
        }

        Toast.makeText(this, "Added 60 minutes to the session", Toast.LENGTH_SHORT).show()
    }

    private fun fetchIpAddress() {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        Handler(Looper.getMainLooper()).postDelayed({
            val request = Request.Builder()
                .url("https://ipinfo.io/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle failure silently to avoid UI interruption
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    response.body?.close()
                    runOnUiThread {
                        if (response.isSuccessful && responseData != null) {
                            try {
                                val jsonObject = JSONObject(responseData)
                                val ip = jsonObject.getString("ip")
                                ipAddress.text = "IP: $ip"

                                val storedIp = sharedPreferences.getString("storedIp", "0.0.0.0")
                                if (storedIp == "0.0.0.0") {
                                    sharedPreferences.edit()
                                        .putString("storedIp", ip)
                                        .apply()
                                }
                            } catch (e: Exception) {
                                // Handle failure silently
                            }
                        } else {
                            ipAddress.text = "Unknown IP"
                        }
                    }
                }
            })
        }, 2000)
    }

    private val ipStatusChecker = object : Runnable {
        override fun run() {
            fetchAndCheckIpAddress()
            if (!isVpnDisconnected) {
                handler.postDelayed(this, ipCheckInterval)
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
                disconnectVPN()
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
                disconnectVPN()
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

    private fun disconnectVPN() {
        try {
            stopTimer()
            handler.removeCallbacksAndMessages(null)
            secondaryHandler.removeCallbacksAndMessages(null)

            val disconnectIntent = Intent(this, de.blinkt.openvpn.core.OpenVPNService::class.java)
            disconnectIntent.action = de.blinkt.openvpn.core.OpenVPNService.DISCONNECT_VPN
            startService(disconnectIntent)

            val remainingMinutesInMillis = (timeLeftInMillis / 1000 / 60) * 60 * 1000
            sharedPreferences.edit()
                .putLong("timeLeftInMillis", remainingMinutesInMillis)
                .putBoolean("isTimerRunning", false)
                .putBoolean("isConnected", false)
                .apply()

            Toast.makeText(this, "VPN Disconnected...", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                redirectToMainActivity()
            }, 1000)
        } catch (e: Exception) {
            // Handle failure silently
        }
    }

    private fun redirectToMainActivity() {
        if (!isFinishing) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        secondaryHandler.removeCallbacksAndMessages(null)
    }
}
