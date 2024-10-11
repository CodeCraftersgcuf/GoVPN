package com.example.privatevpn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
import java.util.concurrent.TimeUnit

class DisconnectActivity : AppCompatActivity() {

    // Declare UI components
    private lateinit var backHome: ImageView
    private lateinit var profileImage: CircleImageView
    private lateinit var countryNameTextView: TextView
    private lateinit var durationTextView: TextView
    private lateinit var ipAddressTextView: TextView
    private lateinit var back_to_home: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disconnect)

        // Initialize UI components
        backHome = findViewById(R.id.back_home)
        profileImage = findViewById(R.id.profile_image)
        countryNameTextView = findViewById(R.id.countryNameTextView)
        durationTextView = findViewById(R.id.durationTextView)
        ipAddressTextView = findViewById(R.id.ipAddressTextView)
        back_to_home = findViewById(R.id.back_to_home) // Missing initialization added

        // Set onClickListener for backHome ImageView
        backHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        // Set onClickListener for back_to_home MaterialButton
        back_to_home.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Fetch data from the intent
        val ipAddress = intent.getStringExtra("IP_ADDRESS") ?: "N/A"
        val durationMillis = intent.getLongExtra("DURATION_MILLIS", 0L)
        val countryName = intent.getStringExtra("COUNTRY_NAME") ?: "Unknown"

        // Format the duration
        val durationFormatted = formatDuration(durationMillis)

        // Set the dynamic data
        setDynamicData(
            countryName = countryName,
            duration = durationFormatted,
            ipAddress = ipAddress
        )
    }

    // Function to set dynamic data
    private fun setDynamicData(
        countryName: String,
        duration: String,
        ipAddress: String
    ) {
        // Determine the correct flag resource ID based on the country name
        val countryFlagResId = when (countryName) {
            "United States" -> R.drawable.us // Image for United States
            "United Kingdom" -> R.drawable.flag // Image for United Kingdom
            else -> R.drawable.default_flag // Use the default flag or current image if no match
        }

        // Set the profile image, country name, duration, and IP address
        profileImage.setImageResource(countryFlagResId)
        countryNameTextView.text = countryName
        durationTextView.text = duration
        ipAddressTextView.text = ipAddress
    }

    // Helper function to format the duration from milliseconds to HH:MM:SS
    private fun formatDuration(durationMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60

        // Log the raw duration and calculated hours, minutes, and seconds
        Log.d("DisconnectActivity", "Raw durationMillis: $durationMillis")
        Log.d("DisconnectActivity", "Calculated Duration - Hours: $hours, Minutes: $minutes, Seconds: $seconds")

        // Return formatted string
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Override the onBackPressed method to remove current activity and navigate to MainActivity
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Close the current activity
    }
}
