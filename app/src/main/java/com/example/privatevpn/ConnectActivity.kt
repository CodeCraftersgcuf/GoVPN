package com.example.privatevpn

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException
import android.os.CountDownTimer
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
            startTimer()  // If already running, just ensure it's started with correct remaining time
        } else if (!isTimerRunning && timeLeftInMillis > 0) {
            updateTimerText()  // Display the saved time
        }

        // Set onClickListeners for the buttons
        setOnClickListeners()
    }

    override fun onResume() {
        super.onResume()

        // Set a dummy IP while fetching the actual one
        ipAddress.text = "IP: 0.0.0.0"

        // Fetch the actual IP after 3 seconds
        Handler().postDelayed({
            fetchIpAddress()
        }, 2000)

        // Restart the timer if the app returns to this activity after disconnection
        if (!isTimerRunning && timeLeftInMillis > 0) {
            startTimer()
        }
    }

    private fun setOnClickListeners() {
        profile_image.setOnClickListener {
            val intent = Intent(this, AllServersActivity::class.java)
            startActivity(intent)
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
            disconnectVPN()
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
                timeLeftInMillis = millisUntilFinished
                updateTimerText()

                // Save the remaining time to SharedPreferences
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
        editor.apply()
    }

    private fun increaseTimerBy60Minutes() {
        // Check if the current time left is already one hour or more
        if (timeLeftInMillis >= 60 * 60 * 1000) { // 60 minutes in milliseconds
            Toast.makeText(this, "Cannot add more time, already over 1 hour", Toast.LENGTH_SHORT).show()
            return
        }

        // Add 60 minutes to the current time left
        timeLeftInMillis += additionalTimeInMillis

        // If the timer is already running, restart it with the new time
        if (isTimerRunning) {
            timer.cancel()
            startTimer()
        } else {
            updateTimerText() // Update the displayed time if not running
            startTimer()
        }

        Toast.makeText(this, "Added 60 minutes to the session", Toast.LENGTH_SHORT).show()
    }

    private fun fetchIpAddress() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://ipinfo.io/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ConnectActivity, "Failed to get IP address", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    if (responseData != null) {
                        val jsonObject = JSONObject(responseData)
                        val ip = jsonObject.getString("ip")
                        ipAddress.text = "IP: $ip"
                    } else {
                        ipAddress.text = "Unknown IP"
                    }
                }
            }
        })
    }

    private fun disconnectVPN() {
        try {
            // Stop the timer before disconnecting
            stopTimer()

            // Disconnect from the VPN
            val disconnectIntent = Intent(this, de.blinkt.openvpn.core.OpenVPNService::class.java)
            disconnectIntent.action = de.blinkt.openvpn.core.OpenVPNService.DISCONNECT_VPN
            startService(disconnectIntent)

            // Save the remaining time after disconnecting
            sharedPreferences.edit().putLong("timeLeftInMillis", timeLeftInMillis).apply()

            Toast.makeText(this, "VPN Disconnected", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()  // Close ConnectActivity so it doesn't appear in the backstack

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to disconnect VPN", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Toast.makeText(this, "Please disconnect VPN before exiting", Toast.LENGTH_SHORT).show()
    }
}
