package com.example.privatevpn

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.material.button.MaterialButton
import okhttp3.*
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
    private var selectedPlan = ""
    private var selectedPrice = ""
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
            val planDuration = plan.getString("duration")
            val planPrice = plan.getInt("price")

            // Create a layout for each plan
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(30, 20, 30, 20) // Add vertical margins for spacing
                }

                // Add padding to the layout (top and bottom)
                setPadding(16, 16, 16, 16) // Adjust the values as needed for your design

                // Create a GradientDrawable for the purple border
                val borderDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setStroke(2, Color.parseColor("#6D4AFF")) // Set border width and color using a direct hex value for purple
                    setColor(Color.TRANSPARENT) // Set background color to transparent
                    cornerRadius = 8f // Optional: Set rounded corners
                }

                background = borderDrawable // Set the background to the created drawable
            }

            // Dynamically create a CheckBox for each plan with a unique ID
            val checkBox = CheckBox(this).apply {
                text = planDuration
                id = View.generateViewId() // Assign a unique ID
            }

            // Create a TextView for the price
            val priceTextView = TextView(this).apply {
                text = "Rs $planPrice.00"
                textSize = 18f
                setTextColor(ContextCompat.getColor(this@subciActivity, R.color.white))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = 16 // Add margin start for spacing between CheckBox and TextView
                }
            }

            // Add CheckBox and Price TextView to the layout
            layout.addView(checkBox)
            layout.addView(priceTextView)

            // Add the created layout to the container
            linearLayoutContainer.addView(layout)

            // Set the onCheckedChangeListener
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Uncheck the last checked checkbox if it exists
                    lastCheckedCheckbox?.isChecked = false

                    // Update total amount and plan amount text view
                    totalAmount = planPrice
                    planAmountTextView.text = "Rs $totalAmount.00"

                    // Set the current checkbox as the last checked
                    lastCheckedCheckbox = checkBox
                } else {
                    // Reset totalAmount if unchecked
                    totalAmount = 0
                    planAmountTextView.text = "Rs 0.00"

                    // Clear the last checked reference if it gets unchecked
                    if (lastCheckedCheckbox == checkBox) {
                        lastCheckedCheckbox = null
                    }
                }
            }
        }
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
                        val paymentData = PaymentData.getFromIntent(data!!)
                        val paymentInfo = paymentData?.toJson()
                        Log.i(TAG, "Payment Successful: $paymentInfo")
                        if (paymentInfo != null) {
                            Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    RESULT_CANCELED -> {
                        Log.i(TAG, "Payment Cancelled")
                        Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
                    }
                    AutoResolveHelper.RESULT_ERROR -> {
                        val errorCode = data?.getStringExtra("com.google.android.gms.wallet.EXTRA_ERROR_CODE")
                        Log.e(TAG, "Payment Failed: Error Code: $errorCode")
                        Toast.makeText(this, "Payment Failed: Error Code: $errorCode", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
