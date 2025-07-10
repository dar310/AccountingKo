// DashboardGraphHelper.kt
package com.css152l_am5_g8.accountingko.ui.dashboard

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.css152l_am5_g8.accountingko.api.ApiClient
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

object DashboardGraphHelper {

    fun loadDashboardGraphs(
        context: Context,
        scope: LifecycleCoroutineScope,
        authManager: AuthManager,
        chartContainer: FrameLayout,
        totalIncomeView: TextView,
        totalExpensesView: TextView,
        netProfitView: TextView
    ) {
        scope.launch {
            try {
                val token = authManager.getToken() ?: return@launch
                val response = ApiClient.apiService.getInvoices("Bearer $token")

                if (response.isSuccessful) {
                    val invoices = response.body()?.data ?: emptyList()

                    // Compute totals
                    val totalIncome = invoices
                        .filter { it.status.lowercase() == "paid" }
                        .sumOf { it.total }

                    val totalExpenses = 85000.0 // Replace with actual API call if needed

                    val netProfit = totalIncome - totalExpenses
                    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

                    totalIncomeView.text = formatter.format(totalIncome)
                    totalExpensesView.text = formatter.format(totalExpenses)
                    netProfitView.text = formatter.format(netProfit)

                    // Show chart (later you can add actual chart logic here)
                    chartContainer.visibility = View.VISIBLE
                    Log.d("DashboardGraphHelper", "Graphs and values loaded successfully")

                } else {
                    Log.e("DashboardGraphHelper", "Failed to fetch invoices")
                }
            } catch (e: Exception) {
                Log.e("DashboardGraphHelper", "Error loading graph data", e)
            }
        }
    }
}