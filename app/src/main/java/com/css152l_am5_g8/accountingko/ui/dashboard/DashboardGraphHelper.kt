package com.css152l_am5_g8.accountingko.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

object DashboardGraphHelper {

    fun loadDashboardGraphs(
        context: Context,
        scope: LifecycleCoroutineScope,
        chartContainer: FrameLayout,
        totalIncomeView: TextView,
        totalExpensesView: TextView,
        netProfitView: TextView
    ) {
        val authManager = AuthManager(context)

        scope.launch {
            try {
                val token = authManager.getToken() ?: return@launch
                val response = ApiClient.apiService.getInvoices("Bearer $token")

                if (response.isSuccessful) {
                    val invoices = response.body()?.data ?: emptyList()

                    // --- FILTER FOR PENDING ONLY ---
                    val pendingInvoices = invoices.filter { it.status.equals("pending", ignoreCase = true) }

                    // --- CALCULATE TOTALS ---
                    val totalIncome = pendingInvoices.sumOf { it.total }
                    val totalExpenses = 85000.0 // placeholder
                    val netProfit = totalIncome - totalExpenses.toString().toBigDecimal()

                    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
                    totalIncomeView.text = formatter.format(totalIncome)
                    totalExpensesView.text = formatter.format(totalExpenses)
                    netProfitView.text = formatter.format(netProfit)

                    // --- SHOW CHART ---
                    chartContainer.visibility = View.VISIBLE
                    chartContainer.removeAllViews()

                    val barChart = BarChart(context)
                    chartContainer.addView(barChart)

                    val grouped = pendingInvoices.groupBy {
                        it.createdAt.take(7) // "YYYY-MM"
                    }

                    val labels = grouped.keys.sorted()
                    val values = labels.map { label ->
                        grouped[label]?.sumOf { it.total.toDouble() }?.toFloat() ?: 0f
                    }

                    setupBarChart(barChart, labels, values)

                    Log.d("DashboardGraphHelper", "Graphs and values loaded successfully")

                } else {
                    Log.e("DashboardGraphHelper", "Failed to fetch invoices: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("DashboardGraphHelper", "Error loading graph data", e)
            }
        }
    }

    private fun setupBarChart(barChart: BarChart, labels: List<String>, values: List<Float>) {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "Pending Invoices")
        dataSet.color = Color.parseColor("#FFA726") // Orange for pending

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        barChart.data = barData
        barChart.setFitBars(true)
        barChart.description.isEnabled = false
        barChart.setScaleEnabled(false)
        barChart.setDrawGridBackground(false)

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return labels.getOrElse(value.toInt()) { "" }
            }
        }
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)

        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false

        barChart.invalidate()
    }

    // Local copy of AuthManager
    class AuthManager(private val context: Context) {
        companion object {
            private const val PREFS_NAME = "auth"
            private const val TOKEN_KEY = "token"
        }

        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun getToken(): String? = prefs.getString(TOKEN_KEY, null)

        fun clearToken() {
            prefs.edit().remove(TOKEN_KEY).apply()
        }

        fun saveToken(token: String) {
            prefs.edit().putString(TOKEN_KEY, token).apply()
        }
    }
}