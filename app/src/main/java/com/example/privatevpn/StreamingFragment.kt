package com.example.privatevpn

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class StreamingFragment : Fragment() {

    private lateinit var countryListLayout: LinearLayout
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_streaming, container, false)

        countryListLayout = view.findViewById(R.id.countryListLayout)

        // Introduce a delay of 10ms before fetching data
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
            animateDropdownIcon(dropdownIcon, 0f, 180f) // Rotate to 180 degrees when expanded
        } else {
            cityListLayout.visibility = View.GONE
            animateDropdownIcon(dropdownIcon, 180f, 0f) // Rotate back to 0 degrees when collapsed
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
        val vpnUsername = "12312" // Replace with actual VPN username
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
}
