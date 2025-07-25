package com.css152l_am5_g8.accountingko.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object ApiClient {
    suspend fun fetchGistContent(url: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            response.body!!.string().trim()
        }
    }
//    val BASE_URL= runBlocking {fetchGistContent("https://gist.githubusercontent.com/dar310/576d486f7952046f62236bcb6a850b7f/raw/tunnel_url.txt")}
//    val BASE_URL= runBlocking {fetchGistContent("https://gist.githubusercontent.com/dar310/576d486f7952046f62236bcb6a850b7f/raw/tunnel_url.txt")}
//    fun getBaseUrl(): String = BASE_URL
    private var BASE_URL: String? = null

    fun getBaseUrl(): String {
        if (BASE_URL == null) {
            BASE_URL = runBlocking {
                try {
                    fetchGistContent("https://gist.githubusercontent.com/dar310/667fedc58b2d442fcd878de125a3d2e7/raw/tunnel_url.txt")
                } catch (e: Exception) {
                    Log.e("ApiClient", "Failed to fetch BASE_URL: ${e.message}")
                    "http://10.0.2.2:3000" // fallback
                }
            }
        }
        return BASE_URL!!
    }
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
        .baseUrl(getBaseUrl())
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API service instance
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}