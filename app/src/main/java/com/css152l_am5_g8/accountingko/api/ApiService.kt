package com.css152l_am5_g8.accountingko.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName

// Data classes for requests and responses
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val success: Boolean, val token: String?, val message: String?)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val success: Boolean,
    val message: String?,
    val user: User?
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)

data class Invoice(
    @SerializedName("id")
    val id: String,

    @SerializedName("invoiceName")
    val invoiceName: String,

    @SerializedName("total")
    val total: Double,

    @SerializedName("status")
    val status: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("dueDate")
    val dueDate: Int,

    @SerializedName("fromName")
    val fromName: String,

    @SerializedName("fromEmail")
    val fromEmail: String,

    @SerializedName("fromAddress")
    val fromAddress: String,

    @SerializedName("clientName")
    val clientName: String,

    @SerializedName("clientEmail")
    val clientEmail: String,

    @SerializedName("clientAddress")
    val clientAddress: String,

    @SerializedName("currency")
    val currency: String,

    @SerializedName("invoiceNumber")
    val invoiceNumber: Int,

    @SerializedName("note")
    val note: String?,

    @SerializedName("invoiceItemDescription")
    val invoiceItemDescription: String,

    @SerializedName("invoiceItemQuantity")
    val invoiceItemQuantity: Int,

    @SerializedName("invoiceItemRate")
    val invoiceItemRate: Double,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("mobileUserId")
    val mobileUserId: String?,

    @SerializedName("User")
    val User: InvoiceUser?
)

data class InvoiceUser(
    @SerializedName("id")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("firstName")
    val firstName: String?,

    @SerializedName("lastName")
    val lastName: String?
)

// Fixed response structure to match actual API response
data class InvoicesResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")  // Changed from "invoices" to "data"
    val data: List<Invoice>?,

    @SerializedName("meta")  // Added meta field
    val meta: ResponseMeta?,

    @SerializedName("message")
    val message: String?
)

// Added meta data class
data class ResponseMeta(
    @SerializedName("count")
    val count: Int,

    @SerializedName("user")
    val user: MetaUser?
)

data class MetaUser(
    @SerializedName("id")
    val id: String,

    @SerializedName("linkedUserId")
    val linkedUserId: String,

    @SerializedName("type")
    val type: String
)

// Shared API Interface
interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("api/invoices")
    suspend fun getInvoices(@Header("Authorization") token: String): Response<InvoicesResponse>
}