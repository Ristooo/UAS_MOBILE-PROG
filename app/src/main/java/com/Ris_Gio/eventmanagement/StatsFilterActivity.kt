package com.Ris_Gio.eventmanagement


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.Ris_Gio.eventmanagement.models.Statistics
import com.Ris_Gio.eventmanagement.networks.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.ArrayAdapter
import android.util.Log
import android.widget.LinearLayout

class StatsFilterActivity : AppCompatActivity() {

    // Deklarasi View Statistik
    private lateinit var tvTotal: TextView
    private lateinit var tvUpcoming: TextView
    private lateinit var tvOngoing: TextView
    private lateinit var tvCompleted: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatusMessage: TextView

    // Deklarasi View Filter
    private lateinit var spinnerFilter: Spinner
    private lateinit var btnApplyFilter: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats_filter)

        // Panggil initViews sebelum fetchStatistics
        initViews()
        fetchStatistics() // Memuat data statistik dan menampilkannya

        btnApplyFilter.setOnClickListener {
            applyFilterAndReturn()
        }
    }

    private fun initViews() {
        // --- Statistik View Initialization ---
        // Binding Views ke ID XML
        try {
            tvTotal = findViewById(R.id.tv_stat_total)
            tvUpcoming = findViewById(R.id.tv_stat_upcoming)
            tvOngoing = findViewById(R.id.tv_stat_ongoing)
            tvCompleted = findViewById(R.id.tv_stat_completed)
            progressBar = findViewById(R.id.progress_bar_stats)
            tvStatusMessage = findViewById(R.id.tv_status_message)

            // --- Filter View Initialization ---
            spinnerFilter = findViewById(R.id.spinner_main_filter)
            btnApplyFilter = findViewById(R.id.btn_apply_filter)

            // Setup Spinner Adapter
            ArrayAdapter.createFromResource(
                this,
                R.array.filter_options,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFilter.adapter = adapter
            }
        } catch (e: Exception) {
            // Jika ada View yang tidak ditemukan (misal, typo ID di XML), log error
            Log.e("StatsActivity", "Error initializing views (ID Mismatch): ${e.message}", e)
            Toast.makeText(this, "Kesalahan fatal: ID tampilan tidak ditemukan.", Toast.LENGTH_LONG).show()
        }
    }

    // --- Logika Jaringan: GET Statistics (?stats=1) ---
    private fun fetchStatistics() {
        progressBar.visibility = View.VISIBLE
        tvStatusMessage.visibility = View.GONE // Pastikan pesan error tersembunyi
        val statsContainer = findViewById<LinearLayout>(R.id.ll_stats_container)
        statsContainer.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Panggil endpoint GET /api.php?stats=1
                val response = RetrofitClient.instance.getStatistics()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.data != null) {
                        displayStatistics(response.body()!!.data!!) // Tampilkan data jika sukses
                        statsContainer.visibility = View.VISIBLE
                    } else {
                        tvStatusMessage.text = "Gagal memuat statistik. Server Code: ${response.code()}"
                        tvStatusMessage.visibility = View.VISIBLE
                        Log.e("API_STATS", "Failed to load stats. Code: ${response.code()}")
                        // Tampilkan pesan error jika server merespon dengan kode error
                        Toast.makeText(this@StatsFilterActivity, "Gagal memuat statistik. Server Code: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    // Pesan ini muncul jika GAGAL koneksi ke IP (Server Down/Blokir)
                    Toast.makeText(this@StatsFilterActivity, "Koneksi Gagal: Server API Mati/Diblokir.", Toast.LENGTH_LONG).show()
                    Log.e("API_STATS", "Exception on network call: ${e.message}", e)
                }
            }
        }
    }

    // --- Logika Tampilan Data Statistik ---
    private fun displayStatistics(stats: Statistics) {
        // PENTING: Mengisi TextViews dengan data Statistik
        tvTotal.text = stats.total
        tvUpcoming.text = stats.upcoming
        tvOngoing.text = stats.ongoing
        tvCompleted.text = stats.completed
    }

    // --- Logika Filter ---
    private fun applyFilterAndReturn() {
        val selectedStatus = spinnerFilter.selectedItem.toString()
        var filterValue: String? = null

        if (selectedStatus != "Semua Event") {
            filterValue = selectedStatus
        }

        // Kembalikan hasil filter ke MainActivity
        val resultIntent = Intent().apply {
            putExtra("EXTRA_FILTER_STATUS", filterValue)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}