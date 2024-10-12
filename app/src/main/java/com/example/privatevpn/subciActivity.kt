package com.example.privatevpn

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.material.button.MaterialButton
import org.json.JSONArray
import org.json.JSONObject

const val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
const val TAG = "SubciActivity"

class subciActivity : AppCompatActivity() {
    private lateinit var cros: ImageView
    private lateinit var oneYearPlanCheckBox: CheckBox
    private lateinit var oneMonthPlanCheckBox: CheckBox
    private lateinit var planAmountTextView: TextView
    private lateinit var paymentsClient: PaymentsClient
    private var selectedPlan = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subci)

        // Find views by their ID
        cros = findViewById(R.id.cros)
        oneYearPlanCheckBox = findViewById(R.id.checkBox)
        oneMonthPlanCheckBox = findViewById(R.id.checkBox2)
        planAmountTextView = findViewById(R.id.planAmountTextView)

        // Close the current activity on click
        cros.setOnClickListener {
            finish()
        }

        // Initialize PaymentsClient for Google Pay
        paymentsClient = Wallet.getPaymentsClient(
            this,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST) // Use ENVIRONMENT_TEST for testing
                .build()
        )

        // Checkbox logic
        oneYearPlanCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                oneMonthPlanCheckBox.isChecked = false
                selectedPlan = "1-year"
                planAmountTextView.text = "Rs 4,450.00"
            }
        }

        oneMonthPlanCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                oneYearPlanCheckBox.isChecked = false
                selectedPlan = "1-month"
                planAmountTextView.text = "Rs 6600.00/yr"
            }
        }

        // Set click listener on the "Start Free Trial" button
        findViewById<MaterialButton>(R.id.startTrialButton).setOnClickListener {
            if (selectedPlan.isEmpty()) {
                Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show()
            } else {
                loadPaymentData()
            }
        }
    }

    // Create a PaymentDataRequest for Google Pay
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
                        put("gateway", "stripe") // Replace with your payment gateway
                        put("gatewayMerchantId", "2316-2826-0729") // Your Payments Profile ID
                    })
                })
            })
        }

        val transactionInfo = JSONObject().apply {
            put("totalPrice", totalPrice)
            put("totalPriceStatus", "FINAL")
            put("currencyCode", "INR") // Set to INR for your pricing
        }

        val merchantInfo = JSONObject().apply {
            put("merchantName", "Your Merchant Name") // Replace with your merchant name
        }

        return PaymentDataRequest.fromJson(baseRequest.apply {
            put("allowedPaymentMethods", allowedPaymentMethods)
            put("transactionInfo", transactionInfo)
            put("merchantInfo", merchantInfo)
        }.toString())
    }

    // Load the Google Pay payment sheet
    private fun loadPaymentData() {
        val totalPrice = if (selectedPlan == "1-year") "4450.00" else "6600.00"
        val request = getPaymentDataRequest(totalPrice)

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

    // Handle the result from the Google Pay payment sheet
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
                            // Handle further processing of payment data here
                        }
                    }
                    RESULT_CANCELED -> {
                        Log.i(TAG, "Payment Cancelled")
                        Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
                    }
                    AutoResolveHelper.RESULT_ERROR -> {
                        // Handle error
                        val errorCode = data?.getStringExtra("com.google.android.gms.wallet.EXTRA_ERROR_CODE")
                        Log.e(TAG, "Payment Failed: Error Code: $errorCode")
                        when (errorCode) {
                            "INVALID_PARAMETER" -> {
                                Toast.makeText(this, "Invalid parameters provided for payment.", Toast.LENGTH_SHORT).show()
                            }
                            "MERCHANT_NOT_SUPPORTED" -> {
                                Toast.makeText(this, "The merchant is not supported.", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this, "Payment Failed: The merchant is having trouble accepting your payment. Try using a different payment method.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
}
