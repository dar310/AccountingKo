package com.css152l_am5_g8.accountingko.ui.invoice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.css152l_am5_g8.accountingko.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InvoiceAdapter (
    private var invoices: List<InvoiceRequest>,
    private val onEditClick: (InvoiceRequest) -> Unit,
    private val onDeleteClick: (InvoiceRequest) -> Unit,
    private val onMarkPaidClick: (InvoiceRequest) -> Unit,
    private val onDownloadPDF: (InvoiceRequest) -> Unit
    ) : RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder>() {

        fun updateData(newInvoices: List<InvoiceRequest>) {
            invoices = newInvoices
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_invoice_card, parent, false)
            return InvoiceViewHolder(view)
        }

        override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
            val invoice = invoices[position]

            holder.tvInvoiceName.text = invoice.invoiceName
            holder.tvInvoiceNumber.text = invoice.invoiceNumber.toString()
            holder.tvClientName.text = invoice.clientName
//           Old || holder.tvDate.text = invoice.date
//           Old || holder.tvDueDate.text = invoice.dueDate.toString()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(invoice.date)
            val calendar = Calendar.getInstance()

            if (date != null) {
                calendar.time = date
                calendar.add(Calendar.DAY_OF_MONTH, invoice.dueDate)  // Add days from dueDate int

                holder.tvDate.text = invoice.date
                holder.tvDueDate.text = sdf.format(calendar.time)  // Due date after adding days
            } else {
                holder.tvDate.text = invoice.date
                holder.tvDueDate.text = "Invalid date"
            }
            holder.tvStatus.text = invoice.status
            holder.tvSubtotal.text = "₱%,.2f".format(invoice.total)

            // Status color example (Pending = Orange, Paid = Green, etc)
            holder.tvStatus.setTextColor(
                when (invoice.status.lowercase()) {
                    "paid" -> 0xFF388E3C.toInt() // green
                    "pending" -> 0xFFFF9800.toInt() // orange
                    else -> 0xFF555555.toInt() // gray default
                }
            )

            holder.btnEdit.setOnClickListener { onEditClick(invoice) }
            holder.btnDelete.setOnClickListener { onDeleteClick(invoice) }
            holder.btnMarkAsPaid.setOnClickListener { onMarkPaidClick(invoice) }
            holder.btnDownloadPdf.setOnClickListener { onDownloadPDF(invoice) }
        }

        override fun getItemCount(): Int = invoices.size

        class InvoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvInvoiceName: TextView = itemView.findViewById(R.id.tv_invoice_name)
            val tvInvoiceNumber: TextView = itemView.findViewById(R.id.tv_invoice_number)
            val tvClientName: TextView = itemView.findViewById(R.id.tv_client_name)
            val tvDate: TextView = itemView.findViewById(R.id.tv_date)
            val tvDueDate: TextView = itemView.findViewById(R.id.tv_due_date)
            val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
            val tvSubtotal: TextView = itemView.findViewById(R.id.tv_subtotal)
            val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
            val btnDelete: Button = itemView.findViewById(R.id.btn_delete)
            val btnMarkAsPaid: Button = itemView.findViewById(R.id.btn_mark_paid)
            val btnDownloadPdf: Button = itemView.findViewById(R.id.btn_download_pdf)
        }
    }