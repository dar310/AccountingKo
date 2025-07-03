package com.css152l_am5_g8.accountingko.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: ListView
    private lateinit var contentFrame: FrameLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Data
    private val invoices = mutableListOf<Invoice>()
    private var invoiceAdapter: InvoiceAdapter? = null

    // Utils
    private val authManager by lazy { AuthManager(this) }
    private val currencyFormatter by lazy { NumberFormat.getCurrencyInstance(Locale("en", "PH")) }

    companion object {
        private const val TAG = "DashboardActivity"

        // Navigation items
        private val NAV_ITEMS = listOf("Dashboard", "Invoices", "Reports", "Settings", "Logout")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        setupUI()
        fetchInvoices()
    }

    private fun setupUI() {
        initializeViews()
        setupNavigationDrawer()
        setupSwipeToRefresh()
        setupMenuButton()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        contentFrame = findViewById(R.id.content_frame)
    }

    private fun setupMenuButton() {
        findViewById<ImageButton>(R.id.menu_button).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNavigationDrawer() {
        navView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, NAV_ITEMS)
        navView.setOnItemClickListener { _, _, position, _ ->
            handleNavigationItemClick(NAV_ITEMS[position])
            drawerLayout.closeDrawers()
        }
    }

    private fun handleNavigationItemClick(item: String) {
        when (item) {
            "Dashboard" -> refreshInvoices()
            "Invoices" -> showToast("Invoices selected")
            "Reports" -> showToast("Reports coming soon")
            "Settings" -> showToast("Settings coming soon")
            "Logout" -> performLogout()
        }
    }

    private fun setupSwipeToRefresh() {
        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )
            setOnRefreshListener { refreshInvoices() }
        }
    }

    private fun fetchInvoices() {
        lifecycleScope.launch {
            try {
                showLoadingState()

                val token = authManager.getToken()
                if (token == null) {
                    handleAuthenticationError("Please login again")
                    return@launch
                }

                Log.d(TAG, "Fetching invoices...")
                val response = ApiClient.apiService.getInvoices("Bearer $token")

                if (!response.isSuccessful) {
                    handleHttpError(response.code(), response.errorBody()?.string())
                    return@launch
                }

                val invoicesResponse = response.body()
                if (invoicesResponse == null) {
                    showError("No data received from server")
                    showEmptyState()
                    return@launch
                }

                handleInvoicesResponse(invoicesResponse)

            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchInvoices", e)
                handleNetworkError(e)
            } finally {
                hideLoading()
            }
        }
    }

    private fun handleInvoicesResponse(response: com.css152l_am5_g8.accountingko.api.InvoicesResponse) {
        Log.d(TAG, "Response success: ${response.success}")
        Log.d(TAG, "Data size: ${response.data?.size ?: 0}")

        if (response.success == true) {
            val invoicesList = response.data ?: emptyList()
            updateInvoicesList(invoicesList)
        } else {
            val errorMessage = response.message ?: "Failed to fetch invoices"
            Log.e(TAG, "API returned success=false: $errorMessage")
            showError(errorMessage)
            showEmptyState()
        }
    }

    private fun updateInvoicesList(invoicesList: List<Invoice>) {
        invoices.clear()
        invoices.addAll(invoicesList)

        if (invoices.isEmpty()) {
            Log.d(TAG, "No invoices found - showing empty state")
            showEmptyState()
        } else {
            Log.d(TAG, "Showing ${invoices.size} invoices")
            showInvoiceList()
        }
    }

    private fun refreshInvoices() = fetchInvoices()

    // Error Handling
    private fun handleAuthenticationError(message: String) {
        showError(message)
        authManager.clearToken()
        // TODO: Navigate to login activity
        // navigateToLogin()
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
            val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            showEmptyState()
        }
    }

    private fun handleNetworkError(e: Exception) {
        Log.e(TAG, "Network error", e)
        showError("Network error: ${e.message}")
        showEmptyState()
    }

    // UI State Management
    private fun showLoadingState() {
        swipeRefreshLayout.isRefreshing = true
        contentFrame.removeAllViews()

        try {
            val loadingView = layoutInflater.inflate(R.layout.loading_state, contentFrame, false)
            contentFrame.addView(loadingView)
        } catch (e: Exception) {
            // Fallback if loading_state.xml doesn't exist
            Log.w(TAG, "Loading state layout not found")
        }
    }

    private fun showEmptyState() {
        val view = layoutInflater.inflate(R.layout.dashboard_no_invoices, contentFrame, false)
        contentFrame.removeAllViews()
        contentFrame.addView(view)

        view.findViewById<ImageButton>(R.id.add_invoice_button)?.setOnClickListener {
            // TODO: Navigate to create invoice
            showToast("Create invoice functionality coming soon")
        }
    }

    private fun showInvoiceList() {
        val listView = createInvoiceListView()

        swipeRefreshLayout.removeAllViews()
        swipeRefreshLayout.addView(listView)

        contentFrame.removeAllViews()
        contentFrame.addView(swipeRefreshLayout)
    }

    private fun createInvoiceListView(): ListView {
        return ListView(this).apply {
            divider = null
            dividerHeight = 0
            setPadding(16, 8, 16, 8)

            invoiceAdapter = InvoiceAdapter(this@DashboardActivity, invoices, currencyFormatter) { invoice ->
                showInvoiceDetails(invoice)
            }
            adapter = invoiceAdapter
        }
    }

    private fun hideLoading() {
        swipeRefreshLayout.isRefreshing = false
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Actions
    private fun showInvoiceDetails(invoice: Invoice) {
        // TODO: Navigate to invoice details activity
        val message = """
            Invoice #${invoice.invoiceNumber}
            Client: ${invoice.clientName}
            Amount: ${currencyFormatter.format(invoice.total)}
        """.trimIndent()

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun performLogout() {
        authManager.clearToken()
        showToast("Logged out successfully")
        // TODO: Navigate to login activity
        val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

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

// Simplified and improved Invoice Adapter
class InvoiceAdapter(
    private val context: Context,
    private val invoices: List<Invoice>,
    private val currencyFormatter: NumberFormat,
    private val onItemClick: (Invoice) -> Unit
) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)
    private val dateFormatter = InvoiceDateFormatter()

    companion object {
        private val VIEW_ID_INVOICE_NUMBER = View.generateViewId()
        private val VIEW_ID_CLIENT_NAME = View.generateViewId()
        private val VIEW_ID_AMOUNT = View.generateViewId()
        private val VIEW_ID_STATUS = View.generateViewId()
        private val VIEW_ID_DATE = View.generateViewId()
        private val VIEW_ID_MENU_BUTTON = View.generateViewId()
    }

    override fun getCount(): Int = invoices.size
    override fun getItem(position: Int): Invoice = invoices[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: createInvoiceItemView()
        val invoice = invoices[position]

        bindInvoiceData(view, invoice)
        setupItemClick(view, invoice)

        return view
    }

    private fun bindInvoiceData(view: View, invoice: Invoice) {
        view.findViewById<TextView>(VIEW_ID_INVOICE_NUMBER)?.text = "#${invoice.invoiceNumber}"
        view.findViewById<TextView>(VIEW_ID_CLIENT_NAME)?.text = invoice.clientName
        view.findViewById<TextView>(VIEW_ID_AMOUNT)?.text = currencyFormatter.format(invoice.total)
        view.findViewById<TextView>(VIEW_ID_DATE)?.text = dateFormatter.format(invoice.date)

        val statusView = view.findViewById<TextView>(VIEW_ID_STATUS)
        statusView?.apply {
            text = invoice.status.uppercase()
            setStatusStyle(invoice.status.lowercase())
        }

        view.findViewById<ImageButton>(VIEW_ID_MENU_BUTTON)?.setOnClickListener { button ->
            showInvoiceMenu(invoice, button)
        }
    }

    private fun setupItemClick(view: View, invoice: Invoice) {
        view.setOnClickListener { onItemClick(invoice) }
    }

    private fun createInvoiceItemView(): View {
        val itemView = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 12)
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundResource(android.R.drawable.list_selector_background)
        }

        // Add child views
        itemView.addView(createInvoiceNumberView())
        itemView.addView(createClientNameView())
        itemView.addView(createAmountView())
        itemView.addView(createStatusView())
        itemView.addView(createDateView())
        itemView.addView(createMenuButton())

        return itemView
    }

    private fun createInvoiceNumberView() = TextView(context).apply {
        id = VIEW_ID_INVOICE_NUMBER
        textSize = 16f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(context.getColor(android.R.color.black))
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }

    private fun createClientNameView() = TextView(context).apply {
        id = VIEW_ID_CLIENT_NAME
        textSize = 14f
        setTextColor(context.getColor(android.R.color.black))
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
    }

    private fun createAmountView() = TextView(context).apply {
        id = VIEW_ID_AMOUNT
        textSize = 14f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setTextColor(context.getColor(android.R.color.black))
        gravity = android.view.Gravity.END
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
    }

    private fun createStatusView() = TextView(context).apply {
        id = VIEW_ID_STATUS
        textSize = 12f
        setTypeface(null, android.graphics.Typeface.BOLD)
        gravity = android.view.Gravity.CENTER
        setPadding(12, 6, 12, 6)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(8, 0, 8, 0)
        layoutParams = params
    }

    private fun createDateView() = TextView(context).apply {
        id = VIEW_ID_DATE
        textSize = 12f
        setTextColor(context.getColor(android.R.color.darker_gray))
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
    }

    private fun createMenuButton() = ImageButton(context).apply {
        id = VIEW_ID_MENU_BUTTON
        setImageResource(android.R.drawable.ic_menu_more)
        background = null
        setPadding(8, 8, 8, 8)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun TextView.setStatusStyle(status: String) {
        val (textColor, backgroundColor) = when (status) {
            "paid" -> android.R.color.white to android.R.color.holo_green_dark
            "pending" -> android.R.color.white to android.R.color.holo_orange_dark
            "overdue" -> android.R.color.white to android.R.color.holo_red_dark
            else -> android.R.color.black to android.R.color.darker_gray
        }

        setTextColor(context.getColor(textColor))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(context.getColor(backgroundColor))
            cornerRadius = 16f
        }
    }

    private fun showInvoiceMenu(invoice: Invoice, anchor: View) {
        PopupMenu(context, anchor).apply {
            menu.add("View Details")
            menu.add("Edit")
            menu.add("Delete")

            setOnMenuItemClickListener { item ->
                handleMenuItemClick(item.title.toString(), invoice)
                true
            }
            show()
        }
    }

    private fun handleMenuItemClick(action: String, invoice: Invoice) {
        val message = when (action) {
            "View Details" -> "View details for invoice #${invoice.invoiceNumber}"
            "Edit" -> "Edit invoice #${invoice.invoiceNumber}"
            "Delete" -> "Delete invoice #${invoice.invoiceNumber}"
            else -> "Unknown action"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

// Utility class for date formatting
class InvoiceDateFormatter {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun format(dateString: String): String {
        return try {
            val date = isoFormat.parse(dateString)
            displayFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.w("InvoiceDateFormatter", "Failed to parse date: $dateString", e)
            fallbackDateParsing(dateString)
        }
    }

    private fun fallbackDateParsing(dateString: String): String {
        return try {
            if (dateString.contains("T")) {
                val datePart = dateString.split("T")[0]
                val parts = datePart.split("-")
                if (parts.size == 3) {
                    val calendar = Calendar.getInstance()
                    calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    return displayFormat.format(calendar.time)
                }
            }
            dateString
        } catch (e: Exception) {
            Log.w("InvoiceDateFormatter", "Failed to manually parse date", e)
            dateString
        }
    }
}