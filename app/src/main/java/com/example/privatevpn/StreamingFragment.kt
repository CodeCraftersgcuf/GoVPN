package com.example.privatevpn

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class StreamingFragment : Fragment() {

    private lateinit var countryListLayout: LinearLayout
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_streaming, container, false)

        countryListLayout = view.findViewById(R.id.countryListLayout)

        Handler(Looper.getMainLooper()).postDelayed({
            fetchCountries()
        }, 10)

        return view
    }

    private fun fetchCountries() {
        val request = Request.Builder()
            .url("https://govpn.ai/api/servers/")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val data = JSONArray(responseBody.string())
                    activity?.runOnUiThread {
                        populateCountryList(data)
                    }
                }
            }
        })
    }

    private fun populateCountryList(data: JSONArray) {
        for (i in 0 until data.length()) {
            val countryObject = data.getJSONObject(i)
            val countryName = countryObject.getString("name")
            val flagUrl = countryObject.getString("flag")
            val serversArray = countryObject.getJSONArray("servers")

            // Create a layout for each country
            val countryLayout = layoutInflater.inflate(R.layout.item_country, null) as LinearLayout
            val countryNameTextView = countryLayout.findViewById<TextView>(R.id.countryName)
            val flagImageView = countryLayout.findViewById<ImageView>(R.id.flagIcon)
            val dropdownIcon = countryLayout.findViewById<ImageView>(R.id.dropdownIcon) // Dropdown icon

            countryNameTextView.text = countryName
            Glide.with(this).load(flagUrl).into(flagImageView)

            // Add onClick listener for country
            countryLayout.setOnClickListener {
                toggleCityList(countryLayout, serversArray, countryName, flagUrl, dropdownIcon)
            }

            // Add country layout to the parent
            countryListLayout.addView(countryLayout)
        }
    }

    private fun toggleCityList(
        countryLayout: LinearLayout,
        serversArray: JSONArray,
        countryName: String,
        flagUrl: String,
        dropdownIcon: ImageView
    ) {
        val cityListLayout = countryLayout.findViewById<LinearLayout>(R.id.cityListLayout)
        if (cityListLayout.visibility == View.GONE) {
            cityListLayout.visibility = View.VISIBLE
            populateCityList(cityListLayout, serversArray, countryName, flagUrl)
            animateDropdownIcon(dropdownIcon, 0f, 180f)
        } else {
            cityListLayout.visibility = View.GONE
            animateDropdownIcon(dropdownIcon, 180f, 0f)
        }
    }

    private fun animateDropdownIcon(dropdownIcon: ImageView, from: Float, to: Float) {
        dropdownIcon.animate()
            .rotation(to)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(300)
            .start()
    }

    private fun populateCityList(
        cityListLayout: LinearLayout,
        serversArray: JSONArray,
        countryName: String,
        flagUrl: String
    ) {
        cityListLayout.removeAllViews()
        for (j in 0 until serversArray.length()) {
            val serverObject = serversArray.getJSONObject(j)
            val cityName = serverObject.getString("name")
            val cityId = serverObject.getInt("id")
            val isPremium = serverObject.getString("isPremium").toBoolean()
            val signalUrl = serverObject.getString("signal")  // Dynamic signal icon URL
            val premiumUrl = "https://govpn.ai/storage/icons/premium.png"  // Static premium icon URL

            // Create a vertical LinearLayout to stack cityName and status
            val cityInfoLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f // Take up remaining space in horizontal layout
                )
            }

            // Create the TextView for the city name
            val cityTextView = TextView(context).apply {
                text = cityName
                setTextSize(18f)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            }

            // Create the TextView for Free or Premium status
            val statusTextView = TextView(context).apply {
                text = if (isPremium) "Premium" else "Free"
                setTextSize(13f)
                setTextColor(Color.parseColor("#808080"))  // Hardcoded grey color (#808080)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 6.dpToPx()  // Add top margin of 6dp
                }
            }


            // Add city name and status to the vertical layout
            cityInfoLayout.addView(cityTextView)
            cityInfoLayout.addView(statusTextView)

            // Create a horizontal LinearLayout to hold the city info and icons
            val cityLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 10, 0, 10) // Add vertical spacing
                }
            }

            // Create the ImageView for the signal icon
            val signalImageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(30.dpToPx(), 30.dpToPx())  // Set width and height to 30dp
            }

            // Load the signal icon using Glide
            context?.let {
                Glide.with(it)
                    .load(signalUrl)
                    .into(signalImageView)
            }

            // Create the ImageView for the premium flag (only visible if the server is premium)
            val premiumImageView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(25.dpToPx(), 25.dpToPx()).apply {
                    setMargins(8, 0, 0, 0) // Add left margin between signal and premium icon
                }
                visibility = if (isPremium) View.VISIBLE else View.GONE
            }

            // Load the premium icon using Glide if the server is premium
            if (isPremium) {
                context?.let {
                    Glide.with(it)
                        .load(premiumUrl)
                        .into(premiumImageView)
                }
            }

            // Set the onClickListener for each city item
            cityLayout.setOnClickListener {
                if (isPremium) {
                    checkSubscriptionAndProceed(cityId, countryName, flagUrl)
                } else {
                    requestOvpnFile(cityId, countryName, flagUrl)
                }
            }

            // Add the TextView and both ImageViews to the horizontal LinearLayout
            cityLayout.addView(cityInfoLayout)
            cityLayout.addView(signalImageView)
            cityLayout.addView(premiumImageView)

            // Add the horizontal LinearLayout to the city list layout
            cityListLayout.addView(cityLayout)
        }
    }

    // Extension function to convert dp to pixels safely
    private fun Int.dpToPx(): Int {
        return context?.resources?.displayMetrics?.density?.let { density ->
            (this * density).toInt()
        } ?: this  // Fallback to return the same value in case context is null
    }

    private fun checkSubscriptionAndProceed(serverId: Int, countryName: String, flagUrl: String) {
        // Get the actual device ID (Android ID)
        val deviceId = Settings.Secure.getString(context?.contentResolver, Settings.Secure.ANDROID_ID)

        // Prepare the request body
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            JSONObject().put("deviceId", deviceId).toString()  // Use the actual Android ID
        )

        val client = OkHttpClient()  // Initialize OkHttpClient if not done elsewhere

        // Build the request
        val request = Request.Builder()
            .url("https://govpn.ai/api/checksubscription")
            .post(requestBody)
            .build()

        // Execute the network call
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to check subscription status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        // Consume the response body only once
                        val jsonResponseString = response.body?.string() ?: ""
                        val jsonResponse = JSONObject(jsonResponseString)

                        // Assuming the correct key is "status" based on your earlier response format
                        val isSubscribed = jsonResponse.getBoolean("status")

                        activity?.runOnUiThread {
                            if (isSubscribed) {
                                requestOvpnFile(serverId, countryName, flagUrl)
                            } else {
                                // Show a toast message indicating it's only for premium users
                                Toast.makeText(context, "Only For Premium Users", Toast.LENGTH_SHORT).show()

                                // Create an intent to redirect the user to the subscription activity (subciActivity)
                                val intent = Intent(context, subciActivity::class.java)

                                // Start the subciActivity to allow the user to subscribe for premium plans
                                context?.startActivity(intent)
                            }

                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }


    private fun requestOvpnFile(serverId: Int, countryName: String, flagUrl: String) {
        val vpnUsername = retrieveDeviceId()
        val requestUrl = "https://govpn.ai/api/ovpn-file/$serverId?vpn_username=$vpnUsername"

        val request = Request.Builder()
            .url(requestUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to retrieve OVPN file", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    activity?.runOnUiThread {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            putExtra("SERVER_ID", serverId)
                            putExtra("COUNTRY_NAME", countryName)
                            putExtra("FLAG_URL", flagUrl)
                        }
                        startActivity(intent)
                    }
                }
            }
        })
    }

    private fun retrieveDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            requireContext().contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
}
