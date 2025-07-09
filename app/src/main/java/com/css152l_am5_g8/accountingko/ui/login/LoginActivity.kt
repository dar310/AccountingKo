//package com.css152l_am5_g8.accountingko.ui.login
//
//import android.app.Activity
//import android.content.Context
//import android.content.Intent
//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProvider
//import android.os.Bundle
//import androidx.annotation.StringRes
//import androidx.appcompat.app.AppCompatActivity
//import android.text.Editable
//import android.text.TextWatcher
//import android.view.View
//import android.view.inputmethod.EditorInfo
//import android.widget.EditText
//import android.widget.Toast
//import com.css152l_am5_g8.accountingko.databinding.ActivityLoginBinding
//import com.css152l_am5_g8.accountingko.R
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.http.Body
//import retrofit2.http.POST
//import retrofit2.Response
//import androidx.lifecycle.lifecycleScope
//import com.css152l_am5_g8.accountingko.MainActivity
//import kotlinx.coroutines.launch
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import android.util.Log
//import com.css152l_am5_g8.accountingko.ui.register.RegisterActivity
//
////A template Functionality. this needs to connect to the database
//class LoginActivity : AppCompatActivity() {
//
//    // Updated to match your server's expected field names
//    data class LoginRequest(val email: String, val password: String)  // Changed from username to email
//    data class LoginResponse(val success: Boolean, val token: String?, val message: String?)
//
//    // API Interface
//    interface ApiService {
//        @POST("api/login")  // This will become http://10.0.2.2:3000/api/login
//        suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
//    }
//
//    // API Client with logging
//    private val loggingInterceptor = HttpLoggingInterceptor { message ->
//        Log.d("API", message)
//    }.apply {
//        level = HttpLoggingInterceptor.Level.BODY
//    }
//
//    private val client = OkHttpClient.Builder()
//        .addInterceptor(loggingInterceptor)
//        .build()
//
//    private val apiService = Retrofit.Builder()
//        .baseUrl("http://10.0.2.2:3000/") // Base URL
//        .client(client)
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()
//        .create(ApiService::class.java)
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setContentView(R.layout.activity_login)
//
//        // Find your views
//        val usernameField = findViewById<android.widget.EditText>(R.id.username)
//        val passwordField = findViewById<android.widget.EditText>(R.id.password)
//        val loginButton = findViewById<android.widget.Button>(R.id.loginBtn)
//        val registerButton = findViewById<android.widget.Button>(R.id.register_loginPage)
//
//        registerButton.setOnClickListener{
//            val signupIntent = Intent(this@LoginActivity, RegisterActivity::class.java)
//            startActivity(signupIntent)
//        }
//
//        loginButton.setOnClickListener {
//            val username = usernameField.text.toString().trim()
//            val password = passwordField.text.toString().trim()
//
//            if (username.isEmpty() || password.isEmpty()) {
//                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            performLogin(username, password)
//        }
//
//    }
//
//    private fun performLogin(username: String, password: String) {
//        lifecycleScope.launch {
//            try {
//                // Show loading
//                Toast.makeText(this@LoginActivity, "Logging in...", Toast.LENGTH_SHORT).show()
//
//                // Log the request data for debugging
//                Log.d("Login", "Attempting login with email: $username")
//
//                // Make API call - using email field name
//                val response = apiService.login(LoginRequest(username, password))
//
//                Log.d("Login", "Response code: ${response.code()}")
//                Log.d("Login", "Response body: ${response.body()}")
//
//                if (response.isSuccessful) {
//                    val loginResponse = response.body()
//
//                    if (loginResponse?.success == true) {
//                        // Login successful
//                        val token = loginResponse.token
//
//                        // Save token
//                        saveToken(token)
//
//                        // Show success message
//                        Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
//
//                        // Navigate to main activity
//                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
//                        startActivity(intent)
//                        finish()
//
//                    } else {
//                        // Login failed
//                        Toast.makeText(this@LoginActivity,
//                            loginResponse?.message ?: "Login failed",
//                            Toast.LENGTH_LONG).show()
//                    }
//                } else {
//                    // HTTP error - get error body for more details
//                    val errorBody = response.errorBody()?.string()
//                    Log.e("Login", "HTTP Error ${response.code()}: $errorBody")
//
//                    Toast.makeText(this@LoginActivity,
//                        "Server error: ${response.code()} - Check your credentials",
//                        Toast.LENGTH_LONG).show()
//                }
//
//            } catch (e: Exception) {
//                // Network error
//                Log.e("Login", "Network error", e)
//                Toast.makeText(this@LoginActivity,
//                    "Network error: ${e.message}",
//                    Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun saveToken(token: String?) {
//        if (token != null) {
//            val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
//            prefs.edit().putString("token", token).apply()
//        }
//    }
//
//    // Helper function to get saved token
//    private fun getToken(): String? {
//        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
//        return prefs.getString("token", null)
//    }
//}
package com.css152l_am5_g8.accountingko.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.css152l_am5_g8.accountingko.MainActivity
import com.css152l_am5_g8.accountingko.R
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.css152l_am5_g8.accountingko.api.LoginRequest
import com.css152l_am5_g8.accountingko.ui.dashboard.DashboardActivity
import com.css152l_am5_g8.accountingko.ui.register.RegisterActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Find your views
        val usernameField = findViewById<android.widget.EditText>(R.id.emailInput)
        val passwordField = findViewById<android.widget.EditText>(R.id.passwordInput)
        val loginButton = findViewById<android.widget.Button>(R.id.loginButton)
        val registerButton = findViewById<android.widget.Button>(R.id.registerButton)

        // Pre-fill email if passed from registration
        val prefilledEmail = intent.getStringExtra("email")
        if (!prefilledEmail.isNullOrEmpty()) {
            usernameField.setText(prefilledEmail)
            passwordField.requestFocus()
        }

        registerButton.setOnClickListener {
            val signupIntent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(signupIntent)
        }

        loginButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(username, password)
        }
    }

    private fun performLogin(username: String, password: String) {
        lifecycleScope.launch {
            try {
                // Show loading
                Toast.makeText(this@LoginActivity, "Logging in...", Toast.LENGTH_SHORT).show()

                // Log the request data for debugging
                Log.d("Login", "Attempting login with email: $username")

                // Make API call using shared client
                val response = ApiClient.apiService.login(LoginRequest(username, password))

                Log.d("Login", "Response code: ${response.code()}")
                Log.d("Login", "Response body: ${response.body()}")

                if (response.isSuccessful) {
                    val loginResponse = response.body()

                    if (loginResponse?.success == true) {
                        // Login successful
                        val token = loginResponse.token

                        // Save token
                        saveToken(token)

                        // Show success message
                        Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()

                        // Navigate to main activity
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        // Login failed
                        Toast.makeText(this@LoginActivity,
                            loginResponse?.message ?: "Login failed",
                            Toast.LENGTH_LONG).show()
                    }
                } else {
                    // HTTP error - get error body for more details
                    val errorBody = response.errorBody()?.string()
                    Log.e("Login", "HTTP Error ${response.code()}: $errorBody")

                    Toast.makeText(this@LoginActivity,
                        "Server error: ${response.code()} - Check your credentials",
                        Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                // Network error
                Log.e("Login", "Network error", e)
                Toast.makeText(this@LoginActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveToken(token: String?) {
        if (token != null) {
            val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
            prefs.edit().putString("token", token).apply()
        }
    }

    // Helper function to get saved token
    private fun getToken(): String? {
        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        return prefs.getString("token", null)
    }
}