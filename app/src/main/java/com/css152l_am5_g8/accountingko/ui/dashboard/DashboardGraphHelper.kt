package com.css152l_am5_g8.accountingko.ui.dashboard

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.css152l_am5_g8.accountingko.api.ApiClient
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object DashboardGraphHelper {

    private const val TAG = "DashboardGraphHelper"

    fun loadDashboardGraphs(
        context: Context,
        scope: LifecycleCoroutineScope,
        chartContainer: FrameLayout
    ) {
        val authManager = AuthManager(context)

        scope.launch {
            try {
                val token = authManager.getToken()
                if (token == null) {
                    Log.e(TAG, "No authentication token found")
                    showNoDataMessage(chartContainer)
                    return@launch
                }

                Log.d(TAG, "Fetching invoices for paid invoices chart")
                val response = ApiClient.apiService.getInvoices("Bearer $token")

                if (response.isSuccessful) {
                    val invoicesResponse = response.body()
                    if (invoicesResponse?.success == true) {
                        val allInvoices = invoicesResponse.data ?: emptyList()
                        Log.d(TAG, "Fetched ${allInvoices.size} total invoices")

                        // Filter for PAID invoices in the last 30 days
                        val thirtyDaysAgo = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -30)
                        }.time

                        val paidInvoicesLast30Days = allInvoices.filter { invoice ->
                            val isPaid = invoice.status.equals("paid", ignoreCase = true)
                            val invoiceDate = parseInvoiceDate(invoice.createdAt)
                            val isWithinLast30Days = invoiceDate?.after(thirtyDaysAgo) == true

                            isPaid && isWithinLast30Days
                        }

                        Log.d(TAG, "Found ${paidInvoicesLast30Days.size} paid invoices in last 30 days")

                        // Clear container and show chart
                        chartContainer.visibility = View.VISIBLE
                        chartContainer.removeAllViews()

                        if (paidInvoicesLast30Days.isNotEmpty()) {
                            val lineChart = LineChart(context)
                            chartContainer.addView(lineChart)

                            // Group by date and sum amounts
                            val dailyTotals = paidInvoicesLast30Days.groupBy { invoice ->
                                extractDateString(invoice.createdAt)
                            }.mapValues { (_, invoices) ->
                                invoices.sumOf { it.total.toDouble() }
                            }

                            // Create 30-day timeline
                            val timeline = generateLast30DaysTimeline()
                            val chartData = timeline.map { date ->
                                val amount = dailyTotals[date] ?: 0.0
                                Pair(date, amount.toFloat())
                            }

                            setupLineChart(lineChart, chartData)
                            Log.d(TAG, "Paid invoices chart setup completed")
                        } else {
                            showNoDataMessage(chartContainer, "No paid invoices in the last 30 days")
                        }
                    } else {
                        Log.e(TAG, "API response unsuccessful")
                        showNoDataMessage(chartContainer, "Failed to load data")
                    }
                } else {
                    Log.e(TAG, "HTTP Error: ${response.code()}")
                    showNoDataMessage(chartContainer, "Server error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading paid invoices chart", e)
                showNoDataMessage(chartContainer, "Error loading chart")
            }
        }
    }

    private fun parseInvoiceDate(dateString: String): Date? {
        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )

            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    return sdf.parse(dateString)
                } catch (e: Exception) {
                    continue
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            null
        }
    }

    private fun extractDateString(dateString: String): String {
        return try {
            val date = parseInvoiceDate(dateString)
            if (date != null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(date)
            } else {
                dateString.take(10)
            }
        } catch (e: Exception) {
            dateString.take(10)
        }
    }

    private fun generateLast30DaysTimeline(): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val timeline = mutableListOf<String>()

        for (i in 29 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            timeline.add(dateFormat.format(calendar.time))
        }

        return timeline
    }

    private fun setupLineChart(lineChart: LineChart, chartData: List<Pair<String, Float>>) {
        val entries = chartData.mapIndexed { index, (_, value) ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, "Paid Invoices")
        dataSet.apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawFilled(true)
            fillColor = Color.parseColor("#E8F5E8")
            fillAlpha = 60
            setDrawValues(false)
        }

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setDrawGridBackground(false)
            setPinchZoom(false)

            axisRight.isEnabled = false

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#666666")
                textSize = 9f
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#666666")
                textSize = 8f
                granularity = 1f
                labelRotationAngle = -45f
                setLabelCount(6, false)

                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index >= 0 && index < chartData.size) {
                            val date = chartData[index].first
                            try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val outputFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                                val parsedDate = inputFormat.parse(date)
                                outputFormat.format(parsedDate ?: Date())
                            } catch (e: Exception) {
                                date.substring(5)
                            }
                        } else ""
                    }
                }
            }

            legend.isEnabled = false
        }

        lineChart.invalidate()
    }

    private fun showNoDataMessage(chartContainer: FrameLayout, message: String = "No chart data available.") {
        chartContainer.removeAllViews()

        val textView = TextView(chartContainer.context).apply {
            text = message
            textSize = 12f
            setTextColor(Color.parseColor("#999999"))
            gravity = android.view.Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }

        chartContainer.addView(textView)
        chartContainer.visibility = View.VISIBLE
    }

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