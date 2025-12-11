package com.Ris_Gio.eventmanagement

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.Ris_Gio.eventmanagement.models.Event
import com.Ris_Gio.eventmanagement.networks.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.* // Import untuk Calendar

class EditEventActivity : AppCompatActivity() {

    // Deklarasi View (Sama seperti NewEventActivity)
    private lateinit var etTitle: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var etCapacity: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnSaveChanges: Button
    private lateinit var progressBar: ProgressBar

    private var eventId: String? = null // ID event yang akan di-edit

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_EventManagementApp)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_event)

        // AKTIFKAN TOMBOL UP/KEMBALI KE PARENT ACTIVITY
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 1. Ambil ID dari Intent
        eventId = intent.getStringExtra("EXTRA_EVENT_ID")

        if (eventId == null) {
            Toast.makeText(this, "Event ID tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Inisialisasi View
        initViews()

        // 3. Setup Listener untuk Date dan Time Picker (BARU)
        etDate.setOnClickListener {
            showDatePicker()
        }
        etTime.setOnClickListener {
            showTimePicker()
        }

        // 4. Muat Data Lama (GET by ID)
        fetchEventDetails(eventId!!)

        // 5. Set listener untuk tombol "Simpan Perubahan"
        btnSaveChanges.setOnClickListener {
            executeUpdateEvent(eventId!!)
        }
    }

    // --- FUNGSI DATE AND TIME PICKER (BARU) ---
    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Format Tanggal ke YYYY-MM-DD
            val formattedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            etDate.setText(formattedDate)
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            // Format Waktu ke HH:MM:SS
            val formattedTime = String.format("%02d:%02d:00", selectedHour, selectedMinute)
            etTime.setText(formattedTime)
        }, hour, minute, true).show() // 'true' untuk format 24 jam
    }


    // Inisialisasi semua View components
    private fun initViews() {
        etTitle = findViewById(R.id.et_title)
        etLocation = findViewById(R.id.et_location)
        etDate = findViewById(R.id.et_date)
        etTime = findViewById(R.id.et_time)
        etCapacity = findViewById(R.id.et_capacity)
        etDescription = findViewById(R.id.et_description)
        spinnerStatus = findViewById(R.id.spinner_status)
        btnSaveChanges = findViewById(R.id.btn_save_changes)
        progressBar = findViewById(R.id.progress_bar)

        // Setup Spinner Adapter
        ArrayAdapter.createFromResource(
            this,
            com.Ris_Gio.eventmanagement.R.array.status_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStatus.adapter = adapter
        }
    }

    // --- Logika Jaringan: GET Single Event (READ) ---
    private fun fetchEventDetails(id: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Panggil API untuk mendapatkan detail event berdasarkan ID
                val response = RetrofitClient.instance.getEventById(id)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.data != null) {
                        fillFormWithData(response.body()!!.data!!)
                    } else {
                        Toast.makeText(this@EditEventActivity, "Gagal memuat detail event.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@EditEventActivity, "Koneksi Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    // Helper untuk mengisi form dengan data lama yang didapat dari API
    private fun fillFormWithData(event: Event) {
        val statusOptions = resources.getStringArray(com.Ris_Gio.eventmanagement.R.array.status_options)
        val statusIndex = statusOptions.indexOf(event.status)

        supportActionBar?.title = "Edit Event ID: ${event.id ?: "N/A"}"

        etTitle.setText(event.title)
        etLocation.setText(event.location)
        etDate.setText(event.date)
        etTime.setText(event.time)
        // Kapasitas wajib, jadi jika null, biarkan kosong agar validasi terpicu
        etCapacity.setText(event.capacity?.toString() ?: "")
        etDescription.setText(event.description ?: "")
        if (statusIndex >= 0) {
            spinnerStatus.setSelection(statusIndex) // Pilih status yang sesuai
        }
    }

    // --- Logika Jaringan: PUT Update Event (UPDATE) ---
    private fun executeUpdateEvent(id: String) {
        // 1. Ambil input dari form
        val title = etTitle.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val date = etDate.text.toString().trim()
        val time = etTime.text.toString().trim()
        val capacityStr = etCapacity.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val status = spinnerStatus.selectedItem.toString()

        // 2. Validasi Wajib Isi (Termasuk Kapasitas)
        if (title.isEmpty() || location.isEmpty() || date.isEmpty() || time.isEmpty() || capacityStr.isEmpty()) {
            Toast.makeText(this, "Semua field bertanda * wajib diisi.", Toast.LENGTH_LONG).show()
            return
        }

        // 3. Konversi kapasitas dan pastikan nilainya valid
        val capacityInt = capacityStr.toIntOrNull()
        if (capacityInt == null || capacityInt <= 0) {
            Toast.makeText(this, "Kapasitas harus berupa angka valid (lebih dari 0).", Toast.LENGTH_LONG).show()
            return
        }

        // Catatan: Validasi format tanggal/waktu dihapus karena DatePicker/TimePicker menjamin format YYYY-MM-DD dan HH:MM:SS

        val updatedEventData = Event(
            id = id, // ID wajib disertakan di data class
            title = title,
            date = date,
            time = time,
            location = location,
            capacity = capacityInt,
            description = description.ifEmpty { null },
            status = status
        )

        // Coroutine untuk panggilan API
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Panggil endpoint PUT Update Event (ID di pass via @Query)
                val response = RetrofitClient.instance.updateEvent(id, updatedEventData)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == 200) {
                        Toast.makeText(this@EditEventActivity,
                            "✅ Event Berhasil Diperbarui!", Toast.LENGTH_LONG).show()

                        setResult(RESULT_OK) // Set result OK agar MainActivity me-refresh
                        finish()
                    } else {
                        val message = response.body()?.message ?: "Gagal memperbarui event (400/500)."
                        Toast.makeText(this@EditEventActivity,
                            "Gagal: $message", Toast.LENGTH_LONG).show()
                        Log.e("API_UPDATE", "Error: ${response.code()}, Message: $message")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditEventActivity, "Koneksi Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}