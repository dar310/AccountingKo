package com.css152l_am5_g8.accountingko.ui.invoice
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.css152l_am5_g8.accountingko.R
import com.css152l_am5_g8.accountingko.ui.login.LoginActivity
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CreateInvoiceActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val apiBaseUrl = ApiClient.getBaseUrl()

    // Add AuthManager
    private val authManager by lazy { AuthManager(this) }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Views
    private lateinit var invoiceName: EditText
    private lateinit var invoiceNumber: EditText
    private lateinit var fromName: EditText
    private lateinit var fromEmail: EditText
    private lateinit var clientName: EditText
    private lateinit var clientEmail: EditText
    private lateinit var invoiceDate: EditText
    private lateinit var dueDate: EditText
    private lateinit var description: EditText
    private lateinit var quantity: EditText
    private lateinit var rate: EditText
    private lateinit var amount: EditText
    private lateinit var note: EditText
    private lateinit var subtotal: TextView
    private lateinit var createBtn: Button

    private var selectedDate: Date? = null
    private var selectedDueDate = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check authentication first
        if (!checkAuthentication()) {
            return
        }

        setContentView(R.layout.invoice_create)

        bindViews()
        setupListeners()
        prefillDefaults()
    }

    private fun checkAuthentication(): Boolean {
        val token = authManager.getToken()
        if (token == null) {
            // Redirect to login
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return false
        }
        return true
    }

    private fun bindViews() {
        invoiceName = findViewById(R.id.cr_invoiceName)
        invoiceNumber = findViewById(R.id.cr_invoiceNumber)
        fromName = findViewById(R.id.cr_fromName)
        fromEmail = findViewById(R.id.cr_fromEmail)
        clientName = findViewById(R.id.cr_clientName)
        clientEmail = findViewById(R.id.cr_clientEmail)
        invoiceDate = findViewById(R.id.cr_invoiceDate)
        dueDate = findViewById(R.id.cr_invoiceDueDate)
        description = findViewById(R.id.cr_description)
        quantity = findViewById(R.id.cr_quantity)
        rate = findViewById(R.id.cr_rate)
        amount = findViewById(R.id.cr_amount)
        note = findViewById(R.id.cr_invoiceNote)
        subtotal = findViewById(R.id.cr_subtotal)
        createBtn = findViewById(R.id.createInvoiceBtn)
    }

    private fun setupListeners() {
        invoiceDate.setOnClickListener {
            showDatePicker { date ->
                selectedDate = date
                invoiceDate.setText(dateFormat.format(date))
            }
        }

        dueDate.setOnClickListener {
            showDueDateDialog()
        }

        quantity.addTextChangedListener(inputWatcher)
        rate.addTextChangedListener(inputWatcher)

        createBtn.setOnClickListener {
            if (validateInputs()) {
                submitInvoice()
            }
        }
    }

    private val inputWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) = calculateSubtotal()
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun prefillDefaults() {
        selectedDate = Date()
        invoiceDate.setText(dateFormat.format(selectedDate!!))
        dueDate.setText("30 days")
        invoiceNumber.setText((1000..9999).random().toString())
        calculateSubtotal()
    }

    private fun calculateSubtotal() {
        val q = quantity.text.toString().toDoubleOrNull() ?: 0.0
        val r = rate.text.toString().toDoubleOrNull() ?: 0.0
        val total = q * r
        amount.setText(String.format("%.2f", total))
        subtotal.text = "Subtotal: ₱${String.format("%.2f", total)}"
    }

    private fun showDatePicker(onSelect: (Date) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            onSelect(cal.time)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showDueDateDialog() {
        val options = arrayOf("Due on Receipt", "15 days", "30 days")
        val values = arrayOf(1, 15, 30)
        AlertDialog.Builder(this)
            .setTitle("Select Due Date")
            .setItems(options) { _, i ->
                selectedDueDate = values[i]
                dueDate.setText(options[i])
            }
            .show()
    }

    private fun validateInputs(): Boolean {
        val fields = listOf(
            invoiceName to "Invoice Name",
            invoiceNumber to "Invoice Number",
            fromName to "From Name",
            fromEmail to "From Email",
            clientName to "Client Name",
            clientEmail to "Client Email",
            invoiceDate to "Date",
            description to "Description",
            quantity to "Quantity",
            rate to "Rate"
        )

        for ((field, name) in fields) {
            if (field.text.isNullOrBlank()) {
                field.error = "$name is required"
                field.requestFocus()
                return false
            }
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(fromEmail.text).matches()) {
            fromEmail.error = "Invalid email"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(clientEmail.text).matches()) {
            clientEmail.error = "Invalid email"
            return false
        }

        return true
    }

    private fun submitInvoice() {
        val token = authManager.getToken()
        if (token == null) {
            handleAuthenticationError("Session expired. Please login again.")
            return
        }

        createBtn.isEnabled = false
        createBtn.text = "Submitting..."

        val invoice = InvoiceRequest(
            invoiceName = invoiceName.text.toString().trim(),
            total = (quantity.text.toString().toDouble() * rate.text.toString().toDouble()).toInt(),
            status = "PENDING",
            date = isoDateFormat.format(selectedDate ?: Date()),
            dueDate = selectedDueDate,
            fromName = fromName.text.toString().trim(),
            fromEmail = fromEmail.text.toString().trim(),
            fromAddress = "",
            clientName = clientName.text.toString().trim(),
            clientEmail = clientEmail.text.toString().trim(),
            clientAddress = "",
            currency = "PHP",
            invoiceNumber = invoiceNumber.text.toString().toInt(),
            note = note.text.toString().trim().takeIf { it.isNotEmpty() },
            invoiceItemDescription = description.text.toString().trim(),
            invoiceItemQuantity = quantity.text.toString().toInt(),
            invoiceItemRate = rate.text.toString().toInt()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = gson.toJson(invoice)
                val requestBody = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$apiBaseUrl/api/invoices")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer $token")  // Add token here
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread { showError("Network error: ${e.message}") }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        runOnUiThread {
                            when (response.code) {
                                200, 201 -> {
                                    Toast.makeText(this@CreateInvoiceActivity, "Invoice created successfully!", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                                401 -> {
                                    handleAuthenticationError("Session expired. Please login again.")
                                }
                                403 -> {
                                    showError("Access denied")
                                }
                                else -> {
                                    val errorBody = response.body?.string()
                                    showError("Server error: ${response.code} - $errorBody")
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                runOnUiThread { showError("Error: ${e.message}") }
            }
        }
    }

    private fun handleAuthenticationError(message: String) {
        showError(message)
        authManager.clearToken()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        createBtn.isEnabled = true
        createBtn.text = "Create Invoice"
    }

    override fun onDestroy() {
        super.onDestroy()
        client.dispatcher.executorService.shutdown()
    }
}

// AuthManager class (same as in DashboardActivity)
class AuthManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "auth"
        private const val TOKEN_KEY = "token"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getToken(): String? = prefs.getString(TOKEN_KEY, null)

    fun clearToken() {
        prefs.edit().remove(TOKEN_KEY).apply()
    }

    fun saveToken(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }
}