package com.example.privatevpn

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.material.button.MaterialButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

const val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
const val TAG = "SubciActivity"

class subciActivity : AppCompatActivity() {
    private lateinit var cros: ImageView
    private lateinit var planAmountTextView: TextView
    private lateinit var paymentsClient: PaymentsClient
    private lateinit var linearLayoutContainer: LinearLayout
    private var selectedPlanId: Int? = null // Store the selected plan ID
    private var totalAmount = 0
    private var lastCheckedCheckbox: CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subci)

        cros = findViewById(R.id.cros)
        planAmountTextView = findViewById(R.id.planAmountTextView)
        linearLayoutContainer = findViewById(R.id.linearLayout1)

        cros.setOnClickListener {
            finish()
        }

        paymentsClient = Wallet.getPaymentsClient(
            this,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()
        )

        fetchPlans()

        findViewById<MaterialButton>(R.id.startTrialButton).setOnClickListener {
            if (totalAmount == 0) {
                Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show()
            } else {
                loadPaymentData()
            }
        }
    }

    private fun fetchPlans() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://govpn.ai/api/plans")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@subciActivity, "Failed to load plans", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "API call failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (!response.isSuccessful || responseData.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@subciActivity, "No plans available", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val plans = JSONArray(responseData)
                runOnUiThread {
                    updateUIWithPlans(plans)
                }
            }
        })
    }
    private fun updateUIWithPlans(plans: JSONArray) {
        if (plans.length() == 0) {
            planAmountTextView.text = "No Plan is available"
            planAmountTextView.visibility = View.VISIBLE
            return
        }

        linearLayoutContainer.removeAllViews()

        for (i in 0 until plans.length()) {
            val plan = plans.getJSONObject(i)
            val planTitle = plan.getString("title")    // Get the plan title
            val planDuration = plan.getString("duration") // Get the plan duration
            val planPrice = plan.getInt("price")       // Get the plan price
            val planId = plan.getInt("id")             // Get the plan ID

            // Create a vertical layout for each plan
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(30, 20, 30, 20) // Add vertical margins for spacing
                }

                // Add padding to the layout (top and bottom)
                setPadding(16, 16, 16, 16)

                // Create a GradientDrawable for the purple border
                val borderDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setStroke(2, Color.parseColor("#6D4AFF")) // Purple border
                    setColor(Color.TRANSPARENT)               // Transparent background
                    cornerRadius = 8f                         // Rounded corners
                }

                background = borderDrawable
            }

            // Create a horizontal layout for the title and checkbox
            val titleAndCheckboxLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Create a TextView for the title
            val titleTextView = TextView(this).apply {
                text = planTitle  // Set the plan title
                textSize = 18f
                setTextColor(ContextCompat.getColor(this@subciActivity, R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f // Weight of 1 to take up remaining space
                )
            }

            // Create a CheckBox for the plan with a unique ID, aligned to the right
            val checkBox = CheckBox(this).apply {
                text = planDuration  // Set plan duration in checkbox
                id = View.generateViewId()  // Assign a unique ID
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END // Align checkbox to the end
                }
            }

            // Ensure checkbox is aligned to the right side by setting gravity in the parent layout
            titleAndCheckboxLayout.gravity = Gravity.END

            // Add the title TextView and CheckBox to the horizontal layout
            titleAndCheckboxLayout.addView(titleTextView)
            titleAndCheckboxLayout.addView(checkBox)

            // Create a TextView for the price (below the title and checkbox)
            val priceTextView = TextView(this).apply {
                text = "Rs $planPrice.00"  // Set price
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@subciActivity, R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8 // Optional: Add margin between price and title/checkbox
                }
            }

            // Add the title and checkbox layout and the price TextView to the main layout
            layout.addView(titleAndCheckboxLayout) // Add the horizontal layout (title + checkbox)
            layout.addView(priceTextView)          // Add the price below

            // Add the created layout to the container
            linearLayoutContainer.addView(layout)

            // Set the onCheckedChangeListener for each CheckBox
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Uncheck the last checked checkbox if it exists
                    lastCheckedCheckbox?.isChecked = false

                    // Update total amount and plan amount text view
                    totalAmount = planPrice
                    planAmountTextView.text = "Rs $totalAmount.00"
                    selectedPlanId = planId // Store the selected plan ID

                    // Set the current checkbox as the last checked
                    lastCheckedCheckbox = checkBox
                } else {
                    // Reset totalAmount if unchecked
                    totalAmount = 0
                    planAmountTextView.text = "Rs 0.00"

                    // Clear the last checked reference if it gets unchecked
                    if (lastCheckedCheckbox == checkBox) {
                        lastCheckedCheckbox = null
                        selectedPlanId = null // Reset selected plan ID
                    }
                }
            }
        }
    }

    private fun retrieveDeviceId(): String {
        return android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
    }

    private fun getPaymentDataRequest(totalPrice: String): PaymentDataRequest? {
        val baseRequest = JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
        }

        val allowedPaymentMethods = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "CARD")
                put("parameters", JSONObject().apply {
                    put("allowedAuthMethods", JSONArray().apply {
                        put("PAN_ONLY")
                        put("CRYPTOGRAM_3DS")
                    })
                    put("allowedCardNetworks", JSONArray().apply {
                        put("AMEX")
                        put("MASTERCARD")
                        put("VISA")
                    })
                })
                put("tokenizationSpecification", JSONObject().apply {
                    put("type", "PAYMENT_GATEWAY")
                    put("parameters", JSONObject().apply {
                        put("gateway", "stripe")
                        put("gatewayMerchantId", "2316-2826-0729")
                    })
                })
            })
        }

        val transactionInfo = JSONObject().apply {
            put("totalPrice", totalPrice)
            put("totalPriceStatus", "FINAL")
            put("currencyCode", "INR")
        }

        val merchantInfo = JSONObject().apply {
            put("merchantName", "Your Merchant Name")
        }

        return PaymentDataRequest.fromJson(baseRequest.apply {
            put("allowedPaymentMethods", allowedPaymentMethods)
            put("transactionInfo", transactionInfo)
            put("merchantInfo", merchantInfo)
        }.toString())
    }

    private fun loadPaymentData() {
        val request = getPaymentDataRequest(totalAmount.toString())

        if (request != null) {
            AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(request),
                this,
                LOAD_PAYMENT_DATA_REQUEST_CODE
            )
        } else {
            Log.e(TAG, "Payment Data Request is null")
            Toast.makeText(this, "Payment Data Request is null", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK -> {
                        data?.let {
                            val paymentData = PaymentData.getFromIntent(data)
                            handlePaymentSuccess(paymentData)
                        }
                    }
                    RESULT_CANCELED -> {
                        Log.e(TAG, "Payment cancelled")
                    }
                    AutoResolveHelper.RESULT_ERROR -> {
                        Log.e(TAG, "Error in payment")
                    }
                }
            }
        }
    }

    private fun handlePaymentSuccess(paymentData: PaymentData?) {
        paymentData?.let {
            val paymentMethodToken = it.paymentMethodToken
            if (paymentMethodToken != null) {
                val token = paymentMethodToken.token
                // Handle the token as needed, such as sending it to your server
                sendSubscriptionRequest()
            } else {
                Toast.makeText(this, "Payment failed, try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSubscriptionRequest() {
        val client = OkHttpClient()
        val requestBodyJson = JSONObject().apply {
            put("plan_id", selectedPlanId) // Use selected plan ID
            put("deviceId", retrieveDeviceId()) // Get the device ID
        }.toString()

        val request = Request.Builder()
            .url("https://govpn.ai/api/subscribe")
            .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@subciActivity, "Failed to subscribe", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Subscription request failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@subciActivity, "Subscription successful", Toast.LENGTH_SHORT).show()
                        // Optionally, redirect to another activity or perform further actions
                        finish() // Close the subscription activity
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@subciActivity, "Failed to subscribe", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
