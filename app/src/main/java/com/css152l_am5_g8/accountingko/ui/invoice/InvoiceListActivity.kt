package com.css152l_am5_g8.accountingko.ui.invoice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.css152l_am5_g8.accountingko.api.Invoice
import com.css152l_am5_g8.accountingko.api.InvoicesResponse
import com.css152l_am5_g8.accountingko.databinding.InvoiceListBinding
import com.css152l_am5_g8.accountingko.api.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InvoiceListActivity : AppCompatActivity() {

    private lateinit var binding: InvoiceListBinding
    private lateinit var adapter: InvoiceAdapter
    private var invoiceList: List<InvoiceRequest> = emptyList()
//    private lateinit var createInvoiveButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = InvoiceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        fetchInvoices()
    }

    private fun setupToolbar() {
//        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Invoice Manager"
    }

    private fun setupRecyclerView() {
        adapter = InvoiceAdapter(invoiceList,
            onEditClick = { invoice ->
                val intent = Intent(this, EditInvoiceActivity::class.java).apply {
                    putExtra("invoice", invoice) // Make sure InvoiceRequest is Parcelable
                }
                startActivity(intent)
            },
            onDeleteClick = { invoice ->
                showDeleteConfirmationDialog(invoice)
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
                            adapter.updateData(invoiceList)
                            showEmptyState(false)
                            updateStats(invoiceList)
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

    private fun showEmptyState(show: Boolean) {
        binding.emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvInvoiceList.visibility = if (show) View.GONE else View.VISIBLE
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
                        adapter.updateData(invoiceList)
                        updateStats(invoiceList)
                        showEmptyState(invoiceList.isEmpty())
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



}
