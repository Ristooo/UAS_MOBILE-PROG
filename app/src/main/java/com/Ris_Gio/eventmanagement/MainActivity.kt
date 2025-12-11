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
import android.content.Context

class MainActivity : AppCompatActivity(), EventAdapter.OnEventActionListener {

    private lateinit var rvEvents: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var eventAdapter: EventAdapter
    private lateinit var fabAddEvent: FloatingActionButton

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
            R.id.action_search_edit -> {
                // Aksi: Cari Event berdasarkan ID dan muat Edit Activity
                promptForEventIdForEdit()
                true
            }
            R.id.action_delete_event -> { // BARU: Menangani aksi Hapus Event
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

    // --- Fungsi Prompt Input ID untuk Edit ---
    private fun promptForEventIdForEdit() { // Nama fungsi diperjelas
        val inputEditText = EditText(this)
        inputEditText.hint = "Masukkan Event ID (Contoh: 123)"

        AlertDialog.Builder(this)
            .setTitle("Cari Event untuk Edit (Berdasarkan ID)")
            .setView(inputEditText)
            .setPositiveButton("Cari") { dialog, _ ->
                val eventId = inputEditText.text.toString().trim()
                if (eventId.isNotEmpty()) {
                    loadEventForEdit(eventId)
                } else {
                    Toast.makeText(this, "ID Event tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- Fungsi Prompt Input ID untuk Hapus (BARU) ---
    private fun promptForEventIdForDelete() {
        val inputEditText = EditText(this)
        inputEditText.hint = "Masukkan Event ID untuk dihapus"

        AlertDialog.Builder(this)
            .setTitle("Hapus Event (Berdasarkan ID)")
            .setView(inputEditText)
            .setPositiveButton("Hapus") { _, _ ->
                val eventId = inputEditText.text.toString().trim()
                if (eventId.isNotEmpty()) {
                    // Panggil onDeleteClicked (yang akan memanggil executeDelete setelah konfirmasi)
                    onDeleteClicked(eventId)
                } else {
                    Toast.makeText(this, "ID Event tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- Fungsi Navigasi ke Edit Event (Muat Event Berdasarkan ID) ---
    private fun loadEventForEdit(eventId: String) {
        val intent = Intent(this, EditEventActivity::class.java).apply {
            putExtra("EXTRA_EVENT_ID", eventId)
        }
        editEventResultLauncher.launch(intent)
    }

    // --- Implementasi Item Click (Detail View) ---
    override fun onItemClicked(event: Event) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            // Mengirim seluruh objek Event ke Activity Detail
            putExtra("EXTRA_EVENT_DETAIL", event)
        }
        startActivity(intent)
    }

    // --- Logika Jaringan: GET All Events ---
    private fun fetchEvents(statusFilter: String? = null) {
        progressBar.visibility = View.VISIBLE

        val statusParam = if (statusFilter.isNullOrEmpty() || statusFilter == "Semua Event") null else statusFilter

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Mengirim hanya status filter ke API
                val response = RetrofitClient.instance.getAllEvents(status = statusParam)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        val events = response.body()?.data
                        if (!events.isNullOrEmpty()) {
                            eventAdapter.updateData(events)
                        } else {
                            Toast.makeText(this@MainActivity, "Data event kosong atau tidak ditemukan.", Toast.LENGTH_LONG).show()
                            eventAdapter.updateData(emptyList()) // Kosongkan daftar
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