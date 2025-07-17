package com.css152l_am5_g8.accountingko.ui.invoice

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
@Parcelize
data class InvoiceRequest(
    val id: String? = null,
    val invoiceName: String,
    val total: BigDecimal,
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
    val invoiceItemRate: BigDecimal
) : Parcelable