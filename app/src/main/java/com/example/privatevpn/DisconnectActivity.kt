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


import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
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

            duration = durationFormatted,
            ipAddress = ipAddress
        )
    }

    // Function to set dynamic data
    private fun setDynamicData(
        duration: String,
        ipAddress: String
    ) {
        val client = OkHttpClient()
        val url = "https://govpn.ai/api/servers" // Your API URL

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    // Load default data if API fails
                    profileImage.setImageDrawable(null)
                    countryNameTextView.text = "Unknown"
                    durationTextView.text = duration
                    ipAddressTextView.text = ipAddress
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val jsonResponse = JSONArray(responseBody)
                        var countryFlagUrl: String? = null
                        var fetchedCountryName: String = "Unknown"

                        // Iterate through the API response to match the IP address
                        for (i in 0 until jsonResponse.length()) {
                            val countryObject = jsonResponse.getJSONObject(i)
                            val serversArray = countryObject.getJSONArray("servers")

                            for (j in 0 until serversArray.length()) {
                                val serverObject = serversArray.getJSONObject(j)
                                val serverIp = serverObject.getString("ip_address")

                                if (serverIp == ipAddress) {
                                    // If IP matches, get the country name and flag
                                    fetchedCountryName = countryObject.getString("name")
                                    countryFlagUrl = countryObject.getString("flag")
                                    break
                                }
                            }

                            if (fetchedCountryName != "Unknown") {
                                break
                            }
                        }

                        // Update the UI with the fetched country flag and information
                        runOnUiThread {
                            if (countryFlagUrl != null) {
                                // Use Glide to load the flag image from the URL
                                Glide.with(this@DisconnectActivity) // Replace with your activity name
                                    .load(countryFlagUrl)
                                    .into(profileImage)
                            } else {
                                // Use default flag if the country is not found
                                profileImage.setImageDrawable(null)
                            }

                            // Set the country name, duration, and IP address
                            countryNameTextView.text = fetchedCountryName
                            durationTextView.text = duration
                            ipAddressTextView.text = ipAddress
                        }
                    } else {
                        // Handle when response body is null
                        runOnUiThread {
                            profileImage.setImageDrawable(null)
                            countryNameTextView.text = "Unknown"
                            durationTextView.text = duration
                            ipAddressTextView.text = ipAddress
                        }
                    }
                } else {
                    // Handle unsuccessful response
                    runOnUiThread {
                        profileImage.setImageDrawable(null)
                        countryNameTextView.text = "Unknown"
                        durationTextView.text = duration
                        ipAddressTextView.text = ipAddress
                    }
                }
            }
        })
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
