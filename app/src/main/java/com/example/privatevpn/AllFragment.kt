package com.example.privatevpn

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class AllFragment : Fragment() {

    private lateinit var countryListLayout: LinearLayout
    private lateinit var loadingIndicator: ProgressBar // Declare ProgressBar
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_all, container, false)

        countryListLayout = view.findViewById(R.id.countryListLayout)
        loadingIndicator = view.findViewById(R.id.loadingIndicator) // Initialize ProgressBar

        // Get the passed server data from the Bundle
        val serverData = arguments?.getString("SERVER_DATA")

        // Show loading indicator
        loadingIndicator.visibility = View.VISIBLE

        // Populate the country list using the passed server data
        if (serverData != null) {
            try {
                val data = JSONArray(serverData)
                populateCountryList(data)
            } catch (e: JSONException) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to parse server data", Toast.LENGTH_SHORT).show()
            } finally {
                loadingIndicator.visibility = View.GONE // Hide loading indicator
            }
        } else {
            Toast.makeText(context, "No server data received", Toast.LENGTH_SHORT).show()
            loadingIndicator.visibility = View.GONE // Hide loading indicator
        }

        return view
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
            val dropdownIcon = countryLayout.findViewById<ImageView>(R.id.dropdownIcon)

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
            animateDropdownIcon(dropdownIcon, 0f, 180f) // Rotate to 180 degrees when expanded
        } else {
            cityListLayout.visibility = View.GONE
            animateDropdownIcon(dropdownIcon, 180f, 0f) // Rotate back to 0 degrees when collapsed
        }
    }

    private fun animateDropdownIcon(dropdownIcon: ImageView, from: Float, to: Float) {
        val rotateAnimation = dropdownIcon.animate()
            .rotation(to)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(300)
        rotateAnimation.start()
    }

    private fun populateCityList(
        cityListLayout: LinearLayout,
        serversArray: JSONArray,
        countryName: String,
        flagUrl: String
    ) {
        cityListLayout.removeAllViews() // Clear previous city list
        for (j in 0 until serversArray.length()) {
            val serverObject = serversArray.getJSONObject(j)
            val cityName = serverObject.getString("name")
            val cityId = serverObject.getInt("id")

            // Create a TextView for each city
            val cityTextView = layoutInflater.inflate(R.layout.item_city, null) as TextView
            cityTextView.text = cityName

            // Add onClick listener for city to request OVPN file
            cityTextView.setOnClickListener {
                requestOvpnFile(cityId, countryName, flagUrl)
            }

            // Add city to city list layout
            cityListLayout.addView(cityTextView)
        }
    }

    private fun requestOvpnFile(serverId: Int, countryName: String, flagUrl: String) {
        // Check if the VPN is connected
        val sharedPreferences = requireActivity().getSharedPreferences("vpnPrefs", Context.MODE_PRIVATE)
        val isConnected = sharedPreferences.getBoolean("isConnected", false)

        if (isConnected) {
            if (isAdded) {
                Toast.makeText(context, "Please disconnect before connecting to another server", Toast.LENGTH_SHORT).show()
            }
            return // Exit the function if already connected
        }

        // Request OVPN file for the selected city (serverId)
        val vpnUsername = "12312" // This could be dynamically fetched if needed
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
                        // Pass the server ID and country info to MainActivity and start it
                        val intent = Intent(context, MainActivity::class.java).apply {
                            putExtra("SERVER_ID", serverId) // Pass the server ID
                            putExtra("COUNTRY_NAME", countryName) // Pass the country name
                            putExtra("FLAG_URL", flagUrl) // Pass the flag URL
                        }
                        startActivity(intent)

                        // Close the fragment when transitioning to MainActivity
                        requireActivity().finish()
                    }
                }
            }
        })
    }
}
