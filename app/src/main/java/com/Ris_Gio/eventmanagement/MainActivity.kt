package com.Ris_Gio.eventmanagement

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.Ris_Gio.eventmanagement.EventAdapter
import com.Ris_Gio.eventmanagement.models.Event
import com.Ris_Gio.eventmanagement.networks.RetrofitClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.Menu
import android.view.MenuItem
import com.Ris_Gio.eventmanagement.R
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import android.content.Context
import android.app.DatePickerDialog // Diperlukan untuk Date Picker

class MainActivity : AppCompatActivity(), EventAdapter.OnEventActionListener {

    private lateinit var rvEvents: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var eventAdapter: EventAdapter
    private lateinit var fabAddEvent: FloatingActionButton

    // VARIABEL UNTUK MENYIMPAN FUNGSI CALLBACK PENCARIAN TANGGAL
    private var dateSearchCallback: ((String) -> Unit)? = null

    // Launcher untuk Activity Create
    private val createEventResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchEvents()
            Toast.makeText(this, "Daftar diperbarui setelah membuat event!", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher untuk Activity Filter/Statistik
    private val filterStatsResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val statusFilter = result.data?.getStringExtra("EXTRA_FILTER_STATUS")
            fetchEvents(statusFilter = statusFilter) // Muat ulang dengan filter status baru
            Toast.makeText(this, "Filter diterapkan!", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher untuk Activity Edit
    private val editEventResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchEvents() // Refresh daftar event setelah sukses di-edit
            Toast.makeText(this, "Event berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // PERBAIKAN TEMA: Panggil setTheme sebelum super.onCreate
        setTheme(R.style.Theme_EventManagementApp)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi View
        rvEvents = findViewById(R.id.rv_events)
        progressBar = findViewById(R.id.progress_bar)
        fabAddEvent = findViewById(R.id.fab_add_event)

        // Setup Adapter
        eventAdapter = EventAdapter(emptyList(), this)
        rvEvents.adapter = eventAdapter
        fetchEvents()

        // Tombol Tambah Event (FAB)
        fabAddEvent.setOnClickListener {
            val intent = Intent(this, NewEventActivity::class.java)
            createEventResultLauncher.launch(intent)
        }
    }

    // --- Implementasi Menu Toolbar ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_view_stats -> {
                // Aksi: Lihat Statistik Event
                val intent = Intent(this, StatsFilterActivity::class.java)
                filterStatsResultLauncher.launch(intent)
                true
            }
            R.id.action_search_view -> { // CARI UNTUK VIEW/FILTER
                promptSearchAndShowDetail()
                true
            }
            R.id.action_search_edit_mode -> { // CARI UNTUK EDIT
                promptSearchAndEdit()
                true
            }
            R.id.action_delete_event -> {
                promptForEventIdForDelete()
                true
            }
            R.id.action_view_dashboard -> {
                // Aksi: Kembali ke Dashboard (Reload/Refresh tanpa filter)
                fetchEvents()
                Toast.makeText(this, "Dashboard diperbarui!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // --- HELPER: DATE PICKER DIALOG ---
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format tanggal menjadi YYYY-MM-DD
                val formattedDate = String.format(
                    "%d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1, // Bulan dimulai dari 0
                    selectedDay
                )
                // Panggil callback setelah tanggal dipilih
                dateSearchCallback?.invoke(formattedDate)
                // Reset callback setelah digunakan
                dateSearchCallback = null
            },
            year,
            month,
            day
        ).show()
    }

    // --- FUNGSI 1: CARI & TAMPILKAN DETAIL/FILTER (ID -> DETAIL, Tanggal -> FILTER) ---
    private fun promptSearchAndShowDetail() {
        val inputEditText = EditText(this).apply {
            hint = "Masukkan ID Event"
        }

        AlertDialog.Builder(this)
            .setTitle("Cari Event untuk Dilihat")
            .setView(inputEditText)
            .setPositiveButton("Cari ID") { dialog, _ -> // Tombol untuk Cari ID
                val searchTerm = inputEditText.text.toString().trim()
                if (searchTerm.matches(Regex("^\\d+$"))) {
                    loadSingleEventAndShowDetail(searchTerm)
                } else {
                    Toast.makeText(this, "Masukkan ID (angka) yang valid.", Toast.LENGTH_LONG).show()
                }
            }
            .setNeutralButton("Cari Tanggal") { _, _ -> // Tombol untuk Cari Tanggal (Menggunakan Picker)
                // Set callback untuk filter daftar event utama
                dateSearchCallback = { selectedDate ->
                    fetchEvents(dateFrom = selectedDate, dateTo = selectedDate)
                    Toast.makeText(this, "Daftar Event difilter berdasarkan Tanggal: $selectedDate", Toast.LENGTH_LONG).show()
                }
                showDatePickerDialog() // Panggil pemilih tanggal
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- FUNGSI 2: CARI & MUAT EDIT (HANYA ID - TANPA FILTER TANGGAL) ---
    private fun promptSearchAndEdit() {
        val inputEditText = EditText(this).apply {
            hint = "Masukkan ID Event"
        }

        AlertDialog.Builder(this)
            .setTitle("Cari Event untuk Edit")
            .setView(inputEditText)
            .setPositiveButton("Cari ID") { dialog, _ ->
                val searchTerm = inputEditText.text.toString().trim()
                if (searchTerm.matches(Regex("^\\d+$"))) {
                    // Hanya ID yang diterima, langsung luncurkan Edit Activity
                    launchEditActivityById(searchTerm)
                } else {
                    Toast.makeText(this, "Masukkan ID Event (angka) untuk Edit.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // Helper untuk memvalidasi format tanggal (dipertahankan untuk fungsi lain)
    private fun isValidDateFormat(date: String): Boolean {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.isLenient = false
            format.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- FUNGSI UNTUK MENGAMBIL EVENT TUNGGAL & MELUNCURKAN DETAIL ---
    private fun loadSingleEventAndShowDetail(eventId: String) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Menggunakan getEventById yang ada di EventApiService
                val response = RetrofitClient.instance.getEventById(eventId)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.data != null) {
                        val event = response.body()!!.data!!
                        // Langsung luncurkan Activity Detail
                        val intent = Intent(this@MainActivity, EventDetailActivity::class.java).apply {
                            putExtra("EXTRA_EVENT_DETAIL", event)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "Event ID $eventId tidak ditemukan.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Koneksi Gagal saat mencari event.", Toast.LENGTH_LONG).show()
                    Log.e("API_SEARCH", "Exception: ${e.message}", e)
                }
            }
        }
    }

    // --- FUNGSI UNTUK MELUNCURKAN EDIT ACTIVITY BERDASARKAN ID ---
    private fun launchEditActivityById(id: String) {
        val intent = Intent(this, EditEventActivity::class.java).apply {
            putExtra("EXTRA_EVENT_ID", id)
        }
        editEventResultLauncher.launch(intent)
        Toast.makeText(this, "Memuat Event ID $id untuk Edit...", Toast.LENGTH_SHORT).show()
    }

    // --- Fungsi Prompt Input ID untuk Hapus ---
    private fun promptForEventIdForDelete() {
        val inputEditText = EditText(this)
        inputEditText.hint = "Masukkan Event ID untuk dihapus"

        AlertDialog.Builder(this)
            .setTitle("Hapus Event (Berdasarkan ID)")
            .setView(inputEditText)
            .setPositiveButton("Hapus") { _, _ ->
                val eventId = inputEditText.text.toString().trim()
                if (eventId.isNotEmpty()) {
                    onDeleteClicked(eventId)
                } else {
                    Toast.makeText(this, "ID Event tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }


    // --- Implementasi Item Click (Detail View) ---
    override fun onItemClicked(event: Event) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            putExtra("EXTRA_EVENT_DETAIL", event)
        }
        startActivity(intent)
    }

    // --- Logika Jaringan: GET All Events (MODIFIKASI: Tambah Date Filter) ---
    private fun fetchEvents(statusFilter: String? = null, dateFrom: String? = null, dateTo: String? = null) {
        progressBar.visibility = View.VISIBLE

        val statusParam = if (statusFilter.isNullOrEmpty() || statusFilter == "Semua Event") null else statusFilter

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Mengirim status, date_from, dan date_to ke API
                val response = RetrofitClient.instance.getAllEvents(
                    status = statusParam,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                )
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        val events = response.body()?.data
                        if (!events.isNullOrEmpty()) {
                            eventAdapter.updateData(events)
                        } else {
                            Toast.makeText(this@MainActivity, "Data event kosong atau tidak ditemukan.", Toast.LENGTH_LONG).show()
                            eventAdapter.updateData(emptyList())
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Gagal memuat data: ${response.code()}", Toast.LENGTH_LONG).show()
                        Log.e("API_CALL", "Error Code: ${response.code()}, Message: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Koneksi Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("API_CALL", "Exception: ${e.message}", e)
                }
            }
        }
    }

    override fun onEditClicked(event: Event) {
        event.id?.let { id ->
            val intent = Intent(this, EditEventActivity::class.java).apply {
                putExtra("EXTRA_EVENT_ID", id)
            }
            editEventResultLauncher.launch(intent)
        } ?: Toast.makeText(this, "Event ID hilang, tidak bisa mengedit.", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteClicked(eventId: String) {
        // Logika konfirmasi hapus dipanggil dari adapter atau promptForEventIdForDelete
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Hapus")
            .setMessage("Anda yakin ingin menghapus Event ID: $eventId? Aksi ini tidak dapat dibatalkan.")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                executeDelete(eventId) // Panggil fungsi delete yang sebenarnya
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeDelete(eventId: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.deleteEvent(eventId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.status == 200) {
                        Toast.makeText(this@MainActivity, "✅ Event berhasil dihapus.", Toast.LENGTH_SHORT).show()
                        fetchEvents() // Muat ulang daftar
                    } else {
                        val message = response.body()?.message ?: "Gagal menghapus event."
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Koneksi Gagal saat menghapus: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}