package com.css152l_am5_g8.accountingko.ui.invoice

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.css152l_am5_g8.accountingko.R
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.css152l_am5_g8.accountingko.api.AuthManager
import com.css152l_am5_g8.accountingko.ui.login.LoginActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EditInvoiceActivity : AppCompatActivity(){
    private val client = OkHttpClient()
    private val gson = Gson()
    private val apiBaseUrl = ApiClient.getBaseUrl()
    private val authManager by lazy { AuthManager(this) }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    // views
    private lateinit var etInvoiceName: EditText
    private lateinit var etInvoiceNumber: EditText
    private lateinit var etFromName: EditText
    private lateinit var etFromEmail: EditText
    private lateinit var etFromAddress: EditText
    private lateinit var etClientName: EditText
    private lateinit var etClientEmail: EditText
    private lateinit var etClientAddress: EditText
    private lateinit var etInvoiceDate: EditText
    private lateinit var etDueDate: EditText
    private lateinit var etDescription: EditText
    private lateinit var etQuantity: EditText
    private lateinit var etRate: EditText
    private lateinit var etAmount: EditText
    private lateinit var etNote: EditText
    private lateinit var tvSubtotal: TextView
    private lateinit var btnSave: Button

    private var selectedDate: Date? = null
    private var selectedDueDays: Int = 30
    private lateinit var invoiceRequest: InvoiceRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkAuth()) return

        setContentView(R.layout.invoice_edit)
        bindViews()
        setupListeners()

        invoiceRequest = intent.getParcelableExtra("invoice")
            ?: run {
                finish()
                return
            }

        prefillFields()
    }

    private fun checkAuth(): Boolean {
        if (authManager.getToken() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return false
        }
        return true
    }

    private fun bindViews() {
        etInvoiceName = findViewById(R.id.et_invoice_name)
        etInvoiceNumber = findViewById(R.id.et_invoice_number)
        etFromName = findViewById(R.id.et_from_name)
        etFromEmail = findViewById(R.id.et_from_email)
        etFromAddress = findViewById(R.id.et_fromAddress)
        etClientName = findViewById(R.id.et_client_name)
        etClientEmail = findViewById(R.id.et_client_email)
        etClientAddress = findViewById(R.id.et_clientAddress)
        etInvoiceDate = findViewById(R.id.et_invoice_date)
        etDueDate = findViewById(R.id.et_due_date)
        etDescription = findViewById(R.id.et_description)
        etQuantity = findViewById(R.id.et_quantity)
        etRate = findViewById(R.id.et_rate)
        etAmount = findViewById(R.id.et_amount)
        etNote = findViewById(R.id.et_notes)
        tvSubtotal = findViewById(R.id.tv_subtotal)
        btnSave = findViewById(R.id.btn_save_invoice)
    }

    private fun setupListeners() {
        etInvoiceDate.setOnClickListener { pickDate { date ->
            selectedDate = date
            etInvoiceDate.setText(dateFormat.format(date))
        }}

        etDueDate.setOnClickListener { selectDueDays() }

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = calculateSubtotal()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etQuantity.addTextChangedListener(watcher)
        etRate.addTextChangedListener(watcher)

        btnSave.setOnClickListener { if (validateInputs()) submitUpdate() }
    }

    private fun prefillFields() {
        etInvoiceName.setText(invoiceRequest.invoiceName)
        etInvoiceNumber.setText(invoiceRequest.invoiceNumber.toString())
        etFromName.setText(invoiceRequest.fromName)
        etFromEmail.setText(invoiceRequest.fromEmail)
        etFromAddress.setText(invoiceRequest.fromAddress)
        etClientName.setText(invoiceRequest.clientName)
        etClientEmail.setText(invoiceRequest.clientEmail)
        etClientAddress.setText(invoiceRequest.clientAddress)
        selectedDate = dateFormat.parse(invoiceRequest.date)
        selectedDueDays = invoiceRequest.dueDate
        etInvoiceDate.setText(invoiceRequest.date.take(10))
        val options = arrayOf("Due on Receipt", "15 days", "30 days")
        val values = arrayOf(0, 15, 30)
        val index = values.indexOf(invoiceRequest.dueDate)
        if (index != -1) {
            etDueDate.setText(options[index])
        } else {
            etDueDate.setText("${invoiceRequest.dueDate} days") // fallback
        }
        etDescription.setText(invoiceRequest.invoiceItemDescription)
        etQuantity.setText(invoiceRequest.invoiceItemQuantity.toString())
        etRate.setText(invoiceRequest.invoiceItemRate.toPlainString())
        etNote.setText(invoiceRequest.note)
        calculateSubtotal()
    }

    private fun calculateSubtotal() {
        val q = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
        val r = etRate.text.toString().toDoubleOrNull() ?: 0.0
        val total = q * r
        etAmount.setText(String.format("%.2f", total))
        tvSubtotal.text = "Subtotal: ₱${"%.2f".format(total)}"
    }

    private fun pickDate(onPick: (Date) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y,m,d); onPick(cal.time)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun selectDueDays() {
        val options = arrayOf("Due on Receipt","15 days","30 days")
        val values = arrayOf(0,15,30)
        AlertDialog.Builder(this)
            .setTitle("Select Due Date")
            .setItems(options) { _, i ->
                selectedDueDays = values[i]
                etDueDate.setText(options[i])
            }.show()
    }

    private fun validateInputs(): Boolean {
        val pairs = listOf(
            etInvoiceName to "Invoice Name",
            etFromName to "From Name", etFromEmail to "From Email",
            etClientName to "Client Name", etClientEmail to "Client Email",
            etInvoiceDate to "Date", etQuantity to "Quantity", etRate to "Rate"
        )
        for ((fld,name) in pairs) {
            if (fld.text.isNullOrBlank()) {
                fld.error = "$name required"; fld.requestFocus(); return false
            }
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(etFromEmail.text).matches()) return false.also {
            etFromEmail.error = "Invalid email"
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(etClientEmail.text).matches()) return false.also {
            etClientEmail.error = "Invalid email"
        }
        return true
    }

    private fun submitUpdate() {
        val token = authManager.getToken() ?: return handleAuthError()
        Log.d("EditInvoiceActivity", "Using ID: ${invoiceRequest.id}")

        btnSave.isEnabled = false; btnSave.text = "Saving..."
//        val etNotes = null
        val updated = InvoiceRequest(
            invoiceName = etInvoiceName.text.toString(),
            invoiceNumber = etInvoiceNumber.text.toString().toInt(),
            fromName = etFromName.text.toString(),
            fromEmail = etFromEmail.text.toString(),
            fromAddress = etFromAddress.text.toString(),
            clientName = etClientName.text.toString(),
            clientEmail = etClientEmail.text.toString(),
            clientAddress = etClientAddress.text.toString(),
            date = isoDateFormat.format(selectedDate ?: Date()),
            dueDate = selectedDueDays,
            invoiceItemQuantity = etQuantity.text.toString().toInt(),
            invoiceItemRate = etRate.text.toString().toBigDecimal(),
            invoiceItemDescription = etDescription.text.toString(),
            note = etNote.text.toString().takeIf { it.isNotBlank() },
            status = invoiceRequest.status,
            total = etAmount.text.toString().toBigDecimal(),
            currency = invoiceRequest.currency
        )
        CoroutineScope(Dispatchers.IO).launch {
            val json = gson.toJson(updated)
            val body = json.toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$apiBaseUrl/api/invoices/${invoiceRequest.id ?: return@launch showError("Invalid invoice ID")}")
                .put(body)
                .addHeader("Authorization", "Bearer $token")
                .build()
            client.newCall(req).enqueue(object: Callback {
                override fun onFailure(c: Call, e: IOException) = runOnUiThread {
                    showError("Network error")
                }
                override fun onResponse(c: Call, r: Response) = runOnUiThread {
                    when (r.code) {
                        200 -> { Toast.makeText(this@EditInvoiceActivity,"Updated!",Toast.LENGTH_LONG).show(); finish() }
                        401 -> handleAuthError()
                        else -> showError("Error ${r.code}")
                    }
                }
            })
        }
    }

    private fun handleAuthError() {
        authManager.clearToken()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        btnSave.isEnabled = true; btnSave.text = "Save Changes"
    }

    override fun onDestroy() {
        super.onDestroy()
        client.dispatcher.executorService.shutdown()
    }
}