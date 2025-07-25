package com.css152l_am5_g8.accountingko.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.css152l_am5_g8.accountingko.R
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.css152l_am5_g8.accountingko.api.ApiClient.apiService
import com.css152l_am5_g8.accountingko.api.Invoice
import com.css152l_am5_g8.accountingko.ui.invoice.CreateInvoiceActivity
import com.css152l_am5_g8.accountingko.ui.login.LoginActivity
import com.css152l_am5_g8.accountingko.api.AuthManager
import com.css152l_am5_g8.accountingko.ui.invoice.InvoiceListActivity
//import com.css152l_am5_g8.accountingko.ui.register.RegisterActivity
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue
import androidx.core.content.edit

class DashboardActivity : AppCompatActivity() {

    // UI Components for main dashboard
//    private var totalIncomeTextView: TextView? = null
//    private var totalExpensesTextView: TextView? = null
//    private var netProfitTextView: TextView? = null
    private var chartSpinner: Spinner? = null
    private var chartContainer: FrameLayout? = null
    private lateinit var totalRevenueTextView: TextView
    private lateinit var totalInvoicesTextView: TextView
    private lateinit var paidInvoicesTextView: TextView
    private lateinit var pendingInvoicesTextView: TextView
    private lateinit var btnInvoices: Button
    private lateinit var btnLogout: Button

    // Recent Invoices Container
    private lateinit var recentInvoicesContainer: LinearLayout

    // Data
    private val invoices = mutableListOf<Invoice>()
//    private val transactions = mutableListOf<Transaction>()
//    private val expenses = mutableListOf<Expense>()

    // Utils
    private val authManager by lazy { AuthManager(this) }
    private val currencyFormatter by lazy { NumberFormat.getCurrencyInstance(Locale("en", "PH")) }

    // State tracking
    private var currentLayoutType: LayoutType = LayoutType.MAIN_DASHBOARD

    enum class LayoutType {
        MAIN_DASHBOARD,
        NO_INVOICES
    }

    companion object {
        private const val TAG = "DashboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeApp()
    }

    override fun onResume() {
        super.onResume()
        refreshDashboardData()
    }

    // ====================
    // INITIALIZATION
    // ====================

    private fun initializeApp() {
        setContentView(R.layout.dashboard)
        currentLayoutType = LayoutType.MAIN_DASHBOARD

        setupMainDashboardUI()
        loadDashboardData()
    }

    private fun setupMainDashboardUI() {
        try {
            initializeMainDashboardViews()
            setupEventListeners()
            setupSpinner()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up main dashboard UI", e)
            showError("Failed to initialize dashboard")
        }
    }

    private fun initializeMainDashboardViews() {
        totalRevenueTextView = findViewById(R.id.tv_total_revenue)
        totalInvoicesTextView = findViewById(R.id.tv_total_invoices)
        paidInvoicesTextView = findViewById(R.id.tv_paid_invoices)
        pendingInvoicesTextView = findViewById(R.id.tv_pending_invoices)
        chartContainer = findViewById(R.id.chartContainer)
        btnInvoices = findViewById(R.id.btn_invoices)
        btnLogout = findViewById(R.id.btn_logout)

        // Find the recent invoices container - you'll need to add this ID to your XML
        recentInvoicesContainer = findViewById(R.id.recent_invoices_container)
    }

    private fun setupNoInvoicesUI() {
        val addInvoiceButton = findViewById<Button>(R.id.btn_create_first_invoice)
        addInvoiceButton?.setOnClickListener {
            navigateToCreateInvoice()
        }
    }

    private fun setupEventListeners() {
        btnInvoices.setOnClickListener {
            val listIntent = Intent(this@DashboardActivity, InvoiceListActivity::class.java)
            startActivity(listIntent)
        }
        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            performLogout()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                showToast("Logging out...")
                clearLocalData()
                authManager.clearToken()
                navigateToLoginAfterLogout()
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout", e)
                showError("Logout failed. Please try again.")
            }
        }
    }

    private fun clearLocalData() {
        invoices.clear()
//        transactions.clear()
//        expenses.clear()

        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit { clear() }

        Log.d(TAG, "Local data cleared")
    }

    private fun navigateToLoginAfterLogout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun setupSpinner() {
        val periods = arrayOf("This Month", "Last Month", "This Quarter", "This Year")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        chartSpinner?.adapter = adapter

        chartSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateChartData(periods[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ====================
    // LAYOUT MANAGEMENT
    // ====================

    private fun switchToNoInvoicesLayout() {
        if (currentLayoutType == LayoutType.NO_INVOICES) return

        setContentView(R.layout.dashboard_no_invoices)
        currentLayoutType = LayoutType.NO_INVOICES
        setupNoInvoicesUI()

        Log.d(TAG, "Switched to no invoices layout")
    }

    private fun switchToMainDashboardLayout() {
        if (currentLayoutType == LayoutType.MAIN_DASHBOARD) return

        setContentView(R.layout.dashboard)
        currentLayoutType = LayoutType.MAIN_DASHBOARD
        setupMainDashboardUI()

        Log.d(TAG, "Switched to main dashboard layout")
    }

    // ====================
    // DATA LOADING
    // ====================

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val token = authManager.getToken()
                if (token == null) {
                    handleAuthenticationError("Please login again")
                    return@launch
                }

                fetchInvoices()

                if (invoices.isNotEmpty()) {
                    totalRevenue()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading dashboard data", e)
                showError("Failed to load dashboard data: ${e.message}")
            }
        }
    }

    private fun refreshDashboardData() {
        lifecycleScope.launch {
            try {
                val token = authManager.getToken()
                if (token == null) {
                    handleAuthenticationError("Please login again")
                    return@launch
                }

                invoices.clear()
//                transactions.clear()
//                expenses.clear()

                loadDashboardData()

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing dashboard", e)
                showError("Failed to refresh dashboard: ${e.message}")
            }
        }
    }

    // ====================
    // API CALLS, GRAPH
    // ====================

    private suspend fun fetchInvoices() {
        try {
            val token = authManager.getToken() ?: return
            val response = ApiClient.apiService.getInvoices("Bearer $token")

            if (response.isSuccessful) {
                val invoicesResponse = response.body()
                if (invoicesResponse?.success == true) {
                    invoices.clear()
                    invoices.addAll(invoicesResponse.data ?: emptyList())

                    Log.d(TAG, "Fetched ${invoices.size} invoices")

                    updateInvoiceStats()
                    updateRecentInvoices() // Add this line to update recent invoices

                    try {
                        chartContainer?.let { container ->
                            DashboardGraphHelper.loadDashboardGraphs(
                                context = this@DashboardActivity,
                                scope = lifecycleScope,
                                chartContainer = container
                            )
                        } ?: Log.e(TAG, "Chart container is null")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading chart", e)
                    }

                    totalRevenue()

                } else {
                    Log.e(TAG, "API response unsuccessful")
                }
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching invoices", e)
            showError("Failed to fetch invoices")
        }
    }

    private suspend fun getTotalSumOfInvoices(token: String): BigDecimal {
        val response = apiService.getInvoices("Bearer $token")
        if (response.isSuccessful) {
            val invoices = response.body()?.data ?: emptyList()
            return invoices.fold(BigDecimal.ZERO) { acc, invoice -> acc + invoice.total }
        } else {
            throw Exception("Failed to fetch invoices: ${response.code()}")
        }
    }

    private suspend fun totalRevenue(){
        val token = authManager.getToken()?: return
        val totalRevenue = getTotalSumOfInvoices(token)
        totalRevenueTextView?.text = currencyFormatter.format(totalRevenue)
    }

    // ====================
    // UI UPDATES
    // ====================

    private fun updateInvoiceStats() {
        if (invoices.isEmpty()) {
            switchToNoInvoicesLayout()
            return
        }

        if (currentLayoutType != LayoutType.MAIN_DASHBOARD) {
            switchToMainDashboardLayout()
        }

        try {
            totalInvoicesTextView.text = invoices.size.toString()

            val paidInvoices = invoices.filter { it.status.lowercase() == "paid" }
            paidInvoicesTextView.text = paidInvoices.size.toString()

            val pendingInvoices = invoices.filter { it.status.lowercase() == "pending" }
            pendingInvoicesTextView.text = pendingInvoices.size.toString()

            Log.d(TAG, "Stats updated - Total: ${invoices.size}, Paid: ${paidInvoices.size}, Pending: ${pendingInvoices.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating invoice stats", e)
        }
    }

    private fun updateRecentInvoices() {
        if (currentLayoutType != LayoutType.MAIN_DASHBOARD) return

        try {
            // Clear existing invoice views
            recentInvoicesContainer.removeAllViews()

            // Get the most recent 3 invoices (or less if fewer exist)
            val recentInvoices = invoices
                .sortedByDescending { it.createdAt } // Sort by creation date, most recent first
                .take(3) // Take only the first 3 (most recent)

            Log.d(TAG, "Displaying ${recentInvoices.size} recent invoices")

            // Create views for each recent invoice
            recentInvoices.forEach { invoice ->
                val invoiceView = createRecentInvoiceView(invoice)
                recentInvoicesContainer.addView(invoiceView)
            }

            // If no invoices, show a message
            if (recentInvoices.isEmpty()) {
                val noInvoicesView = createNoRecentInvoicesView()
                recentInvoicesContainer.addView(noInvoicesView)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating recent invoices", e)
        }
    }

    private fun createRecentInvoiceView(invoice: Invoice): View {
        val invoiceLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32 // 12dp margin bottom
            }
        }

        // Create avatar (first 2 letters of client name)
        val avatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(96, 96) // 32dp x 32dp
            text = getClientInitials(invoice.clientName)
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.white, null))
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(getAvatarColor(invoice.clientName))
            setPadding(8, 8, 8, 8)
        }

        // Create client info layout
        val clientInfoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = 36 // 12dp margin start
            }
        }

        // Client name
        val clientName = TextView(this).apply {
            text = invoice.clientName
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(android.R.color.black, null))
        }

        // Client email (if available)
        val clientEmail = TextView(this).apply {
            text = invoice.clientEmail ?: "No email provided"
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        clientInfoLayout.addView(clientName)
        clientInfoLayout.addView(clientEmail)

        // Amount
        val amount = TextView(this).apply {
            text = "+${currencyFormatter.format(invoice.total)}"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
        }

        // Add views to invoice layout
        invoiceLayout.addView(avatar)
        invoiceLayout.addView(clientInfoLayout)
        invoiceLayout.addView(amount)

        return invoiceLayout
    }

    private fun createNoRecentInvoicesView(): View {
        return TextView(this).apply {
            text = "No recent invoices found"
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            gravity = android.view.Gravity.CENTER
            setPadding(16, 32, 16, 32)
        }
    }

    private fun getClientInitials(clientName: String): String {
        return clientName.split(" ")
            .take(2)
            .map { it.firstOrNull()?.uppercaseChar() ?: "" }
            .joinToString("")
            .take(2)
    }

    private fun getAvatarColor(clientName: String): Int {
        val colors = arrayOf(
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_blue_light,
            android.R.color.holo_red_light,
            android.R.color.holo_purple
        )
        val index = clientName.hashCode().absoluteValue % colors.size
        return resources.getColor(colors[index], null)
    }


    private fun updateChartData(period: String) {
        if (currentLayoutType != LayoutType.MAIN_DASHBOARD) return

        Log.d(TAG, "Chart updated for period: $period")
        showToast("Chart updated for $period")
    }

    // ====================
    // USER ACTIONS
    // ====================

    private fun navigateToCreateInvoice() {
        val intent = Intent(this, CreateInvoiceActivity::class.java)
        startActivity(intent)
    }

    // ====================
    // ERROR HANDLING
    // ====================

    private fun handleAuthenticationError(message: String) {
        showError(message)
        authManager.clearToken()
        navigateToLogin()
    }

    private fun handleHttpError(code: Int, errorBody: String?) {
        Log.e(TAG, "HTTP Error $code: $errorBody")

        val message = when (code) {
            401 -> {
                authManager.clearToken()
                "Session expired. Please login again."
            }
            403 -> "Access denied"
            404 -> "Service not found"
            500 -> "Server error. Please try again later."
            else -> "Server error: $code"
        }

        showError(message)
        if (code == 401) {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
    }
}

// ====================
// DATA CLASSES
// ====================

data class Transaction(
    val id: String,
    val description: String,
    val category: String,
    val amount: Double,
    val date: String,
    val type: String // "income" or "expense"
)
