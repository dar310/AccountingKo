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
import com.css152l_am5_g8.accountingko.api.Invoice
import com.css152l_am5_g8.accountingko.ui.invoice.CreateInvoiceActivity
import com.css152l_am5_g8.accountingko.ui.login.LoginActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    // UI Components for main dashboard
    private var searchEditText: EditText? = null
    private var totalIncomeTextView: TextView? = null
    private var totalExpensesTextView: TextView? = null
    private var netProfitTextView: TextView? = null
    private var chartSpinner: Spinner? = null
    private var chartContainer: FrameLayout? = null
    private lateinit var totalRevenueTextView: TextView
    private lateinit var totalInvoicesTextView: TextView
    private lateinit var paidInvoicesTextView: TextView
    private lateinit var pendingInvoicesTextView: TextView

    // Data
    private val invoices = mutableListOf<Invoice>()
    private val transactions = mutableListOf<Transaction>()
    private val expenses = mutableListOf<Expense>()

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
    }

    private fun setupNoInvoicesUI() {
        val addInvoiceButton = findViewById<Button>(R.id.btn_create_first_invoice)
        addInvoiceButton?.setOnClickListener {
            navigateToCreateInvoice()
        }
    }

    private fun setupEventListeners() {
        searchEditText?.setOnEditorActionListener { _, _, _ ->
            performSearch(searchEditText?.text.toString())
            true
        }
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

                // Load invoices first to determine layout
                fetchInvoices()

                // Load other data only if we have invoices (main dashboard)
                if (invoices.isNotEmpty()) {
                    fetchTransactions()
                    fetchExpenses()
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

                // Clear existing data
                invoices.clear()
                transactions.clear()
                expenses.clear()

                // Reload data
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
                    updateInvoiceStats()
                    DashboardGraphHelper.loadDashboardGraphs(
                        context = this,
                        scope = lifecycleScope,
                        authManager = authManager,
                        chartContainer = chartContainer!!,
                        totalIncomeView = totalIncomeTextView!!,
                        totalExpensesView = totalExpensesTextView!!,
                        netProfitView = netProfitTextView!!
                    )
                    updatePendingInvoices()
                }
            } else {
                handleHttpError(response.code(), response.errorBody()?.string())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching invoices", e)
        }
    }

    private suspend fun fetchTransactions() {
        try {
            val token = authManager.getToken() ?: return
            // TODO: Implement API call for transactions
            updateRecentTransactions()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions", e)
        }
    }

    private suspend fun fetchExpenses() {
        try {
            val token = authManager.getToken() ?: return
            // TODO: Implement API call for expenses
            updateExpenseStats()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching expenses", e)
        }
    }

    // ====================
    // UI UPDATES
    // ====================

    private fun updateInvoiceStats() {
        // Check if we need to switch layouts
        if (invoices.isEmpty()) {
            switchToNoInvoicesLayout()
            return
        }

        // Ensure we're on the main dashboard
        if (currentLayoutType != LayoutType.MAIN_DASHBOARD) {
            switchToMainDashboardLayout()
        }

        // Update statistics
        val totalIncome = invoices
            .filter { it.status.lowercase() == "paid" }
            .sumOf { it.total }

        totalIncomeTextView?.text = currencyFormatter.format(totalIncome)
        updateNetProfit()
    }

    private fun updateExpenseStats() {
        if (currentLayoutType != LayoutType.MAIN_DASHBOARD) return

        // TODO: Calculate from actual expense data
        val totalExpenses = 85000.0 // Mock data
        totalExpensesTextView?.text = currencyFormatter.format(totalExpenses)
        updateNetProfit()
    }

    private fun updateNetProfit() {
        if (currentLayoutType != LayoutType.MAIN_DASHBOARD) return

        val incomeText = totalIncomeTextView?.text?.toString() ?: "0"
        val expenseText = totalExpensesTextView?.text?.toString() ?: "0"

        val income = parseCurrencyValue(incomeText)
        val expenses = parseCurrencyValue(expenseText)

        val netProfit = income - expenses
        netProfitTextView?.text = currencyFormatter.format(netProfit)
    }

    private fun updateRecentTransactions() {
        if (currentLayoutType != LayoutType.MAIN_DASHBOARD) return

        // TODO: Implement dynamic transaction cards
        Log.d(TAG, "Recent transactions updated")
    }

    private fun updatePendingInvoices() {
        if (currentLayoutType != LayoutType.MAIN_DASHBOARD) return

        // TODO: Implement dynamic invoice cards
        Log.d(TAG, "Pending invoices updated")
    }

    private fun updateChartData(period: String) {
        if (currentLayoutType != LayoutType.MAIN_DASHBOARD) return

        // TODO: Implement chart data update based on selected period
        Log.d(TAG, "Chart updated for period: $period")
        showToast("Chart updated for $period")
    }

    // ====================
    // USER ACTIONS
    // ====================

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            showToast("Please enter a search query")
            return
        }

        // TODO: Implement search functionality
        Log.d(TAG, "Searching for: $query")
        showToast("Searching for: $query")
    }

    private fun navigateToCreateInvoice() {
        // TODO: Replace with actual CreateInvoiceActivity navigation
        showToast("Navigate to Create Invoice")

        // Example navigation:
        val intent = Intent(this, CreateInvoiceActivity::class.java)
        startActivity(intent)
    }

    // ====================
    // UTILITY METHODS
    // ====================

    private fun parseCurrencyValue(currencyText: String): Double {
        return try {
            currencyText.replace("[^\\d.]".toRegex(), "").toDouble()
        } catch (e: Exception) {
            0.0
        }
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

data class Expense(
    val id: String,
    val description: String,
    val category: String,
    val amount: Double,
    val date: String
)

// ====================
// AUTH MANAGER
// ====================

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