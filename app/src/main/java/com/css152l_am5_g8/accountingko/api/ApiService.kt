package com.css152l_am5_g8.accountingko.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

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

// Shared API Interface
interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
}