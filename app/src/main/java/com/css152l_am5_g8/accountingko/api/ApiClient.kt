package com.css152l_am5_g8.accountingko.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:3000/"
    fun getBaseUrl(): String = BASE_URL
    // Logging interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("API", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp client with logging
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API service instance
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}