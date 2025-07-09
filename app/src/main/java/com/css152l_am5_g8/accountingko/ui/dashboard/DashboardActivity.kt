package com.css152l_am5_g8.accountingko.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.css152l_am5_g8.accountingko.R
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.css152l_am5_g8.accountingko.api.Invoice
import com.css152l_am5_g8.accountingko.ui.login.LoginActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    // UI Components
    private lateinit var searchEditText: EditText
    private lateinit var totalIncomeTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var netProfitTextView: TextView
    private lateinit var chartSpinner: Spinner
    private lateinit var chartContainer: FrameLayout
//    private lateinit var newInvoiceCard: CardView
//    private lateinit var addExpenseCard: CardView
//    private lateinit var viewAllTransactionsText: TextView
//    private lateinit var viewAllInvoicesText: TextView

    // Data
    private val invoices = mutableListOf<Invoice>()
    private val transactions = mutableListOf<Transaction>()
    private val expenses = mutableListOf<Expense>()

    // Utils
    private val authManager by lazy { AuthManager(this) }
    private val currencyFormatter by lazy { NumberFormat.getCurrencyInstance(Locale("en", "PH")) }

    companion object {
        private const val TAG = "DashboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        setupUI()
        loadDashboardData()
    }

    private fun setupUI() {
        initializeViews()
        setupEventListeners()
        setupSpinner()
    }

    private fun initializeViews() {
        searchEditText = findViewById(R.id.et_search)
        totalIncomeTextView = findViewById(R.id.tv_total_income)
        totalExpensesTextView = findViewById(R.id.tv_total_expenses)
        netProfitTextView = findViewById(R.id.tv_net_profit)
        chartSpinner = findViewById(R.id.spinner_chart_period)
        chartContainer = findViewById(R.id.chart_container)
//
//        // Quick action cards — these must exist in dashboard.xml
//        newInvoiceCard = findViewById(R.id.card_new_invoice)
//        addExpenseCard = findViewById(R.id.card_add_expense)
//
//        // View all links — these must exist in dashboard.xml
//        viewAllTransactionsText = findViewById(R.id.tv_view_all_transactions)
//        viewAllInvoicesText = findViewById(R.id.tv_view_all_invoices)
    }


    private fun setupEventListeners() {
        // Search functionality
        searchEditText.setOnEditorActionListener { _, _, _ ->
            performSearch(searchEditText.text.toString())
            true
        }

        // Quick action cards
//        newInvoiceCard.setOnClickListener {
//            // TODO: Navigate to create invoice activity
//            showToast("Create New Invoice")
//        }
//
//        addExpenseCard.setOnClickListener {
//            // TODO: Navigate to add expense activity
//            showToast("Add New Expense")
//        }
//
//        // View all links
//        viewAllTransactionsText.setOnClickListener {
//            // TODO: Navigate to transactions activity
//            showToast("View All Transactions")
//        }
//
//        viewAllInvoicesText.setOnClickListener {
//            // TODO: Navigate to invoices activity
//            showToast("View All Invoices")
//        }
    }

    private fun setupSpinner() {
        val periods = arrayOf("This Month", "Last Month", "This Quarter", "This Year")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        chartSpinner.adapter = adapter

        chartSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateChartData(periods[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val token = authManager.getToken()
                if (token == null) {
                    handleAuthenticationError("Please login again")
                    return@launch
                }

                // Load all dashboard data concurrently
                fetchInvoices()
                fetchTransactions()
                fetchExpenses()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading dashboard data", e)
                showError("Failed to load dashboard data: ${e.message}")
            }
        }
    }

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
            // For now, using mock data
            updateRecentTransactions()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions", e)
        }
    }

    private suspend fun fetchExpenses() {
        try {
            val token = authManager.getToken() ?: return
            // TODO: Implement API call for expenses
            // For now, using mock data
            updateExpenseStats()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching expenses", e)
        }
    }

    private fun updateInvoiceStats() {
        val totalIncome = invoices
            .filter { it.status.lowercase() == "paid" }
            .sumOf { it.total }

        totalIncomeTextView.text = currencyFormatter.format(totalIncome)
        updateNetProfit()
    }

    private fun updateExpenseStats() {
        // TODO: Calculate from actual expense data
        val totalExpenses = 85000.0 // Mock data
        totalExpensesTextView.text = currencyFormatter.format(totalExpenses)
        updateNetProfit()
    }

    private fun updateNetProfit() {
        val incomeText = totalIncomeTextView.text.toString()
        val expenseText = totalExpensesTextView.text.toString()

        // Parse currency values (remove currency symbols and parse)
        val income = parseCurrencyValue(incomeText)
        val expenses = parseCurrencyValue(expenseText)

        val netProfit = income - expenses
        netProfitTextView.text = currencyFormatter.format(netProfit)
    }

    private fun parseCurrencyValue(currencyText: String): Double {
        return try {
            currencyText.replace("[^\\d.]".toRegex(), "").toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun updateRecentTransactions() {
        // TODO: Implement dynamic transaction cards
        // For now, the XML has static transaction cards
        Log.d(TAG, "Recent transactions updated")
    }

    private fun updatePendingInvoices() {
        // TODO: Implement dynamic invoice cards
        // For now, the XML has static invoice cards
        Log.d(TAG, "Pending invoices updated")
    }

    private fun updateChartData(period: String) {
        // TODO: Implement chart data update based on selected period
        Log.d(TAG, "Chart updated for period: $period")
        showToast("Chart updated for $period")
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            showToast("Please enter a search query")
            return
        }

        // TODO: Implement search functionality
        Log.d(TAG, "Searching for: $query")
        showToast("Searching for: $query")
    }

    // Error Handling
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

    override fun onBackPressed() {
        super.onBackPressed()
    }
}

// Data classes for transactions and expenses
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

// Separate Auth Manager for better organization
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