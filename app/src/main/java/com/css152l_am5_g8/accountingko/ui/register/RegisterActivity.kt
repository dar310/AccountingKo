//package com.css152l_am5_g8.accountingko.ui.register
//
//import android.content.Context
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.widget.Button
//import android.widget.EditText
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import com.css152l_am5_g8.accountingko.R
//import com.css152l_am5_g8.accountingko.ui.login.LoginActivity
//import kotlinx.coroutines.launch
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Response
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.http.Body
//import retrofit2.http.POST
//import android.util.Patterns
//
//class RegisterActivity : AppCompatActivity() {
//
//    // Data classes for registration
//    data class RegisterRequest(
//        val name: String,
//        val email: String,
//        val password: String
//    )
//
//    data class RegisterResponse(
//        val success: Boolean,
//        val message: String?,
//        val user: User?
//    )
//
//    data class User(
//        val id: Int,
//        val name: String,
//        val email: String
//    )
//
//    // API Interface
//    interface ApiService {
//        @POST("api/register")
//        suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
//    }
//
//    // API Client setup
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
//        .baseUrl("http://10.0.2.2:3000/")
//        .client(client)
//        .addConverterFactory(GsonConverterFactory.create())
//        .build()
//        .create(ApiService::class.java)
//
//    // UI Components
//    private lateinit var firstNameField: EditText
//    private lateinit var lastNameField: EditText
//    private lateinit var emailField: EditText
//    private lateinit var passwordField: EditText
//    private lateinit var confirmPasswordField: EditText
//    private lateinit var registerButton: Button
//    private lateinit var loginRedirectText: TextView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_register)
//
//        initializeViews()
//        setupClickListeners()
//    }
//
//    private fun initializeViews() {
//        firstNameField = findViewById(R.id.f_name)
//        lastNameField = findViewById(R.id.l_name)
//        emailField = findViewById(R.id.username)
//        passwordField = findViewById(R.id.password)
//        confirmPasswordField = findViewById(R.id.confirm_password)
//        registerButton = findViewById(R.id.register)
//        loginRedirectText = findViewById(R.id.loginRedirectText)
//    }
//
//    private fun setupClickListeners() {
//        registerButton.setOnClickListener {
//            performRegistration()
//        }
//
//        loginRedirectText.setOnClickListener {
//            // Navigate to login activity
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
//    }
//
//    private fun performRegistration() {
//        // Get field values
//        val firstName = firstNameField.text.toString().trim()
//        val lastName = lastNameField.text.toString().trim()
//        val email = emailField.text.toString().trim()
//        val password = passwordField.text.toString()
//        val confirmPassword = confirmPasswordField.text.toString()
//
//        // Validate inputs
//        if (!validateInputs(firstName, lastName, email, password, confirmPassword)) {
//            return
//        }
//
//        // Combine first and last name for the API
//        val fullName = "$firstName $lastName"
//
//        // Perform registration
//        lifecycleScope.launch {
//            try {
//                // Show loading
//                Toast.makeText(this@RegisterActivity, "Creating account...", Toast.LENGTH_SHORT).show()
//
//                // Disable button to prevent multiple submissions
//                registerButton.isEnabled = false
//
//                // Log the request data for debugging
//                Log.d("Register", "Attempting registration with email: $email, name: $fullName")
//
//                // Make API call
//                val response = apiService.register(RegisterRequest(fullName, email, password))
//
//                Log.d("Register", "Response code: ${response.code()}")
//                Log.d("Register", "Response body: ${response.body()}")
//
//                if (response.isSuccessful) {
//                    val registerResponse = response.body()
//
//                    if (registerResponse?.success == true) {
//                        // Registration successful
//                        Toast.makeText(this@RegisterActivity,
//                            "Account created successfully! Please login.",
//                            Toast.LENGTH_LONG).show()
//
//                        // Navigate to login activity
//                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
//                        // Pass email to login activity to pre-fill
//                        intent.putExtra("email", email)
//                        startActivity(intent)
//                        finish()
//
//                    } else {
//                        // Registration failed
//                        Toast.makeText(this@RegisterActivity,
//                            registerResponse?.message ?: "Registration failed",
//                            Toast.LENGTH_LONG).show()
//                    }
//                } else {
//                    // HTTP error
//                    val errorBody = response.errorBody()?.string()
//                    Log.e("Register", "HTTP Error ${response.code()}: $errorBody")
//
//                    val errorMessage = when (response.code()) {
//                        409 -> "Email already exists. Please use a different email."
//                        400 -> "Invalid registration data. Please check your inputs."
//                        else -> "Server error: ${response.code()}"
//                    }
//
//                    Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
//                }
//
//            } catch (e: Exception) {
//                // Network error
//                Log.e("Register", "Network error", e)
//                Toast.makeText(this@RegisterActivity,
//                    "Network error: ${e.message}",
//                    Toast.LENGTH_LONG).show()
//            } finally {
//                // Re-enable button
//                registerButton.isEnabled = true
//            }
//        }
//    }
//
//    private fun validateInputs(
//        firstName: String,
//        lastName: String,
//        email: String,
//        password: String,
//        confirmPassword: String
//    ): Boolean {
//
//        // Check if any field is empty
//        if (firstName.isEmpty()) {
//            firstNameField.error = "First name is required"
//            firstNameField.requestFocus()
//            return false
//        }
//
//        if (lastName.isEmpty()) {
//            lastNameField.error = "Last name is required"
//            lastNameField.requestFocus()
//            return false
//        }
//
//        if (email.isEmpty()) {
//            emailField.error = "Email is required"
//            emailField.requestFocus()
//            return false
//        }
//
//        if (password.isEmpty()) {
//            passwordField.error = "Password is required"
//            passwordField.requestFocus()
//            return false
//        }
//
//        if (confirmPassword.isEmpty()) {
//            confirmPasswordField.error = "Please confirm your password"
//            confirmPasswordField.requestFocus()
//            return false
//        }
//
//        // Validate email format
//        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//            emailField.error = "Please enter a valid email address"
//            emailField.requestFocus()
//            return false
//        }
//
//        // Validate password length
//        if (password.length < 6) {
//            passwordField.error = "Password must be at least 6 characters"
//            passwordField.requestFocus()
//            return false
//        }
//
//        // Check if passwords match
//        if (password != confirmPassword) {
//            confirmPasswordField.error = "Passwords do not match"
//            confirmPasswordField.requestFocus()
//            return false
//        }
//
//        // Clear any previous errors
//        firstNameField.error = null
//        lastNameField.error = null
//        emailField.error = null
//        passwordField.error = null
//        confirmPasswordField.error = null
//
//        return true
//    }
//}
package com.css152l_am5_g8.accountingko.ui.register

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.css152l_am5_g8.accountingko.R
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.css152l_am5_g8.accountingko.api.RegisterRequest
import com.css152l_am5_g8.accountingko.ui.login.LoginActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    // UI Components
    private lateinit var firstNameField: EditText
    private lateinit var lastNameField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var confirmPasswordField: EditText
    private lateinit var registerButton: Button
    private lateinit var loginRedirectText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        firstNameField = findViewById(R.id.f_name)
        lastNameField = findViewById(R.id.l_name)
        emailField = findViewById(R.id.username)
        passwordField = findViewById(R.id.password)
        confirmPasswordField = findViewById(R.id.confirm_password)
        registerButton = findViewById(R.id.register)
        loginRedirectText = findViewById(R.id.loginRedirectText)
    }

    private fun setupClickListeners() {
        registerButton.setOnClickListener {
            performRegistration()
        }

        loginRedirectText.setOnClickListener {
            // Navigate to login activity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun performRegistration() {
        // Get field values
        val firstName = firstNameField.text.toString().trim()
        val lastName = lastNameField.text.toString().trim()
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString()
        val confirmPassword = confirmPasswordField.text.toString()

        // Validate inputs
        if (!validateInputs(firstName, lastName, email, password, confirmPassword)) {
            return
        }

        // Combine first and last name for the API
        val fullName = "$firstName $lastName"

        // Perform registration
        lifecycleScope.launch {
            try {
                // Show loading
                Toast.makeText(this@RegisterActivity, "Creating account...", Toast.LENGTH_SHORT).show()

                // Disable button to prevent multiple submissions
                registerButton.isEnabled = false

                // Log the request data for debugging
                Log.d("Register", "Attempting registration with email: $email, name: $fullName")

                // Make API call using shared client
                val response = ApiClient.apiService.register(RegisterRequest(fullName, email, password))

                Log.d("Register", "Response code: ${response.code()}")
                Log.d("Register", "Response body: ${response.body()}")

                if (response.isSuccessful) {
                    val registerResponse = response.body()

                    if (registerResponse?.success == true) {
                        // Registration successful
                        Toast.makeText(this@RegisterActivity,
                            "Account created successfully! Please login.",
                            Toast.LENGTH_LONG).show()

                        // Navigate to login activity
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        // Pass email to login activity to pre-fill
                        intent.putExtra("email", email)
                        startActivity(intent)
                        finish()

                    } else {
                        // Registration failed
                        Toast.makeText(this@RegisterActivity,
                            registerResponse?.message ?: "Registration failed",
                            Toast.LENGTH_LONG).show()
                    }
                } else {
                    // HTTP error
                    val errorBody = response.errorBody()?.string()
                    Log.e("Register", "HTTP Error ${response.code()}: $errorBody")

                    val errorMessage = when (response.code()) {
                        409 -> "Email already exists. Please use a different email."
                        400 -> "Invalid registration data. Please check your inputs."
                        else -> "Server error: ${response.code()}"
                    }

                    Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                // Network error
                Log.e("Register", "Network error", e)
                Toast.makeText(this@RegisterActivity,
                    "Network error: ${e.message}",
                    Toast.LENGTH_LONG).show()
            } finally {
                // Re-enable button
                registerButton.isEnabled = true
            }
        }
    }

    private fun validateInputs(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {

        // Check if any field is empty
        if (firstName.isEmpty()) {
            firstNameField.error = "First name is required"
            firstNameField.requestFocus()
            return false
        }

        if (lastName.isEmpty()) {
            lastNameField.error = "Last name is required"
            lastNameField.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            emailField.error = "Email is required"
            emailField.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            passwordField.error = "Password is required"
            passwordField.requestFocus()
            return false
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordField.error = "Please confirm your password"
            confirmPasswordField.requestFocus()
            return false
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.error = "Please enter a valid email address"
            emailField.requestFocus()
            return false
        }

        // Validate password length
        if (password.length < 6) {
            passwordField.error = "Password must be at least 6 characters"
            passwordField.requestFocus()
            return false
        }

        // Check if passwords match
        if (password != confirmPassword) {
            confirmPasswordField.error = "Passwords do not match"
            confirmPasswordField.requestFocus()
            return false
        }

        // Clear any previous errors
        firstNameField.error = null
        lastNameField.error = null
        emailField.error = null
        passwordField.error = null
        confirmPasswordField.error = null

        return true
    }
}