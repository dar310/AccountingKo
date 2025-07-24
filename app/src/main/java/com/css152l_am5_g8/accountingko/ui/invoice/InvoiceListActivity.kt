package com.css152l_am5_g8.accountingko.ui.invoice

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.css152l_am5_g8.accountingko.api.Invoice
import com.css152l_am5_g8.accountingko.api.InvoicesResponse
import com.css152l_am5_g8.accountingko.databinding.InvoiceListBinding
import com.css152l_am5_g8.accountingko.api.AuthManager
import com.css152l_am5_g8.accountingko.api.StatusUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class InvoiceListActivity : AppCompatActivity() {

    private lateinit var binding: InvoiceListBinding
    private lateinit var adapter: InvoiceAdapter
    private var invoiceList: List<InvoiceRequest> = emptyList()
    private var filteredInvoiceList: List<InvoiceRequest> = emptyList()
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = InvoiceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        setupSearch()
        fetchInvoices()
    }

    private fun setupToolbar() {
        supportActionBar?.title = "Invoice Manager"
    }

    private fun setupRecyclerView() {
        adapter = InvoiceAdapter(filteredInvoiceList,
            onEditClick = { invoice ->
                val intent = Intent(this, EditInvoiceActivity::class.java).apply {
                    putExtra("invoice", invoice) // Make sure InvoiceRequest is Parcelable
                }
                startActivity(intent)
            },
            onDeleteClick = { invoice ->
                showDeleteConfirmationDialog(invoice)
            },
            onMarkPaidClick = { invoice ->
                showMarkPaidConfirmationDialog(invoice)
            }
        )

        binding.rvInvoiceList.layoutManager = LinearLayoutManager(this)
        binding.rvInvoiceList.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabCreateInvoice.setOnClickListener {
            Toast.makeText(this, "Create Invoice Clicked", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, CreateInvoiceActivity::class.java))
        }

        // Optional: Add filter button functionality
        binding.btnFilter.setOnClickListener {
            // You can implement additional filtering options here
            showFilterDialog()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val searchQuery = s?.toString()?.trim() ?: ""
                currentSearchQuery = searchQuery
                filterInvoices(searchQuery)
            }
        })
    }

    private fun filterInvoices(query: String) {
        filteredInvoiceList = if (query.isEmpty()) {
            invoiceList
        } else {
            invoiceList.filter { invoice ->
                searchInInvoice(invoice, query.lowercase(Locale.getDefault()))
            }
        }

        adapter.updateData(filteredInvoiceList)
        updateStats(filteredInvoiceList)
        showEmptyState(filteredInvoiceList.isEmpty(), query.isNotEmpty())
    }

    private fun searchInInvoice(invoice: InvoiceRequest, query: String): Boolean {
        return invoice.invoiceName.lowercase(Locale.getDefault()).contains(query) ||
                invoice.invoiceNumber.toString().lowercase(Locale.getDefault()).contains(query) ||
                invoice.clientName.lowercase(Locale.getDefault()).contains(query) ||
                invoice.clientEmail.lowercase(Locale.getDefault()).contains(query) ||
                invoice.status.lowercase(Locale.getDefault()).contains(query) ||
                invoice.invoiceItemDescription.lowercase(Locale.getDefault()).contains(query) ||
                invoice.total.toString().contains(query)
    }

    private fun fetchInvoices() {
        val token = AuthManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getInvoices("Bearer $token")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val invoicesResponse: InvoicesResponse? = response.body()
                        val invoices = invoicesResponse?.data ?: emptyList()

                        if (invoices.isEmpty()) {
                            showEmptyState(true)
                        } else {
                            invoiceList = invoices.map { it.toInvoiceRequest() }
                            // Apply current search filter
                            filterInvoices(currentSearchQuery)
                        }
                    } else {
                        Toast.makeText(this@InvoiceListActivity, "Failed to load invoices", Toast.LENGTH_SHORT).show()
                        showEmptyState(true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InvoiceListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showEmptyState(true)
                }
            }
        }
    }

    private fun showEmptyState(show: Boolean, isSearching: Boolean = false) {
        binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvInvoiceList.visibility = if (show) View.GONE else View.VISIBLE

        // Update empty state message based on whether user is searching
        if (show && isSearching) {
            // You might want to update the empty state text for search results
            // This would require modifying your XML or adding additional TextViews
        }
    }

    private fun updateStats(invoices: List<InvoiceRequest>) {
        val totalInvoices = invoices.size
        val totalAmount = invoices.sumOf { it.total }

        binding.tvTotalInvoices.text = totalInvoices.toString()
        binding.tvTotalAmount.text = "₱%,.2f".format(totalAmount)
    }

    // Extension function to convert API Invoice to UI InvoiceRequest
    private fun Invoice.toInvoiceRequest(): InvoiceRequest {
        return InvoiceRequest(
            id = this.id,
            invoiceName = invoiceName,
            invoiceNumber = invoiceNumber,
            clientName = clientName,
            clientAddress = clientAddress,
            clientEmail = clientEmail,
            currency = "PHP",
            fromAddress = fromAddress,
            fromName = fromName,
            fromEmail = fromEmail,
            date = date,
            dueDate = dueDate,
            status = status,
            invoiceItemQuantity = invoiceItemQuantity,
            invoiceItemRate = invoiceItemRate,
            invoiceItemDescription = invoiceItemDescription,
            note = note,
            total = total
        )
    }
    // Add this extension function to your code


    override fun onResume() {
        super.onResume()
        fetchInvoices() // refresh list every time activity resumes
    }

    private fun showDeleteConfirmationDialog(invoice: InvoiceRequest) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Delete Invoice")
        builder.setMessage("Are you sure you want to delete the invoice \"${invoice.invoiceName}\"?")
        builder.setPositiveButton("Delete") { dialog, _ ->
            dialog.dismiss()
            deleteInvoice(invoice)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun showMarkPaidConfirmationDialog(invoice: InvoiceRequest) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Mark as Paid")
        builder.setMessage("Mark this invoice as Paid?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            markInvoiceAsPaid(invoice)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun markInvoiceAsPaid(invoice: InvoiceRequest) {
        // 1. Validate authentication and data
        val token = AuthManager.getToken(this) ?: run {
            Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show()
            return
        }

        val invoiceId = invoice.id ?: run {
            Toast.makeText(this, "Invalid invoice reference", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 2. Execute API call
                val response = ApiClient.apiService.updateInvoiceStatus(
                    "Bearer $token",
                    invoiceId,
                    StatusUpdateRequest(status = "PAID")
                )

                withContext(Dispatchers.Main) {
                    // 3. Handle response
                    if (!response.isSuccessful) {
                        Toast.makeText(this@InvoiceListActivity,
                            "Server error: ${response.code()}",
                            Toast.LENGTH_SHORT).show()
                        return@withContext
                    }

                    val body = response.body()
                    if (body == null) {
                        Toast.makeText(this@InvoiceListActivity,
                            "Empty response from server",
                            Toast.LENGTH_SHORT).show()
                    } else if (!body.success) {
                        Toast.makeText(this@InvoiceListActivity,
                            body.message ?: "Status update failed",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        // Success case
                        invoiceList = invoiceList.map {
                            if (it.id == invoiceId) it.copy(status = "Paid") else it
                        }
                        filterInvoices(currentSearchQuery)
                        Toast.makeText(this@InvoiceListActivity,
                            "Invoice marked as Paid",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InvoiceListActivity,
                        "Error: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT).show()
                    Log.e("InvoiceUpdate", "Status update failed", e)
                }
            }
        }
    }

    private fun deleteInvoice(invoice: InvoiceRequest) {
        val token = AuthManager.getToken(this)
        if (token == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val invoiceId = invoice.id
        if (invoiceId == null) {
            Toast.makeText(this, "Cannot delete invoice: Missing invoice ID", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.deleteInvoice("Bearer $token", invoiceId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@InvoiceListActivity, "Invoice deleted", Toast.LENGTH_SHORT).show()
                        invoiceList = invoiceList.filter { it.id != invoiceId }
                        // Reapply search filter after deletion
                        filterInvoices(currentSearchQuery)
                    } else {
                        Toast.makeText(this@InvoiceListActivity, "Failed to delete invoice", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InvoiceListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showFilterDialog() {
        // Optional: Implement additional filtering options
        // For example: filter by status, date range, amount range, etc.
        val filterOptions = arrayOf("All", "Paid", "Pending")

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Filter by Status")
        builder.setItems(filterOptions) { dialog, which ->
            val selectedFilter = filterOptions[which]
            applyStatusFilter(selectedFilter)
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun applyStatusFilter(status: String) {
        filteredInvoiceList = if (status == "All") {
            invoiceList
        } else {
            invoiceList.filter { invoice ->
                invoice.status.equals(status, ignoreCase = true)
            }
        }

        // If there's an active search query, apply it to the filtered results
        if (currentSearchQuery.isNotEmpty()) {
            filteredInvoiceList = filteredInvoiceList.filter { invoice ->
                searchInInvoice(invoice, currentSearchQuery.lowercase(Locale.getDefault()))
            }
        }

        adapter.updateData(filteredInvoiceList)
        updateStats(filteredInvoiceList)
        showEmptyState(filteredInvoiceList.isEmpty(), currentSearchQuery.isNotEmpty())
    }
}