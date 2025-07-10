package com.css152l_am5_g8.accountingko.ui.invoice
data class InvoiceRequest(
    val invoiceName: String,
    val total: Int,
    val status: String,
    val date: String,
    val dueDate: Int,
    val fromName: String,
    val fromEmail: String,
    val fromAddress: String,
    val clientName: String,
    val clientEmail: String,
    val clientAddress: String,
    val currency: String,
    val invoiceNumber: Int,
    val note: String?,
    val invoiceItemDescription: String,
    val invoiceItemQuantity: Int,
    val invoiceItemRate: Int
)