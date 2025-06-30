package com.css152l_am5_g8.accountingko.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.css152l_am5_g8.accountingko.R

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: ListView
    private lateinit var contentFrame: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        contentFrame = findViewById(R.id.content_frame)

        findViewById<ImageButton>(R.id.menu_button).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Sample nav items
        val items = listOf("Dashboard", "Invoices")
        navView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)

        navView.setOnItemClickListener { _, _, position, _ ->
            when (items[position]) {
                "Dashboard" -> { /* handle dashboard */ }
                "Invoices" -> { /* handle invoices */ }
            }
            drawerLayout.closeDrawers()
        }

        // Simulate: no invoices found
        val invoices = listOf<String>() // or fetch from backend
        if (invoices.isEmpty()) {
            showEmptyState()
        } else {
            showInvoiceList(invoices)
        }
    }

    private fun showEmptyState() {
        val view = layoutInflater.inflate(R.layout.dashboard_no_invoices, contentFrame, false)
        contentFrame.removeAllViews()
        contentFrame.addView(view)
    }

    private fun showInvoiceList(invoices: List<String>) {
        val listView = ListView(this)
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, invoices)
        contentFrame.removeAllViews()
        contentFrame.addView(listView)
    }

    view.findViewById<ImageButton>(R.id.add_invoice_button).setOnClickListener {
        startActivity(Intent(this, CreateInvoiceActivity::class.java))
    }

}