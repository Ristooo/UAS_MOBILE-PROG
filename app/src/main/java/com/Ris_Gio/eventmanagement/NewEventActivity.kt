package com.Ris_Gio.eventmanagement

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.Ris_Gio.eventmanagement.models.Event
import com.Ris_Gio.eventmanagement.networks.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.Ris_Gio.eventmanagement.R
import java.util.* // Import untuk Calendar

class NewEventActivity : AppCompatActivity() {
    private lateinit var etTitle: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var etCapacity: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnCreateEvent: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        // PERBAIKAN TEMA: Panggil setTheme sebelum super.onCreate
        setTheme(R.style.Theme_EventManagementApp)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_event)

        // AKTIFKAN TOMBOL UP/KEMBALI KE PARENT ACTIVITY
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inisialisasi View
        etTitle = findViewById(R.id.et_title)
        etLocation = findViewById(R.id.et_location)
        etDate = findViewById(R.id.et_date)
        etTime = findViewById(R.id.et_time)
        etCapacity = findViewById(R.id.et_capacity)
        etDescription = findViewById(R.id.et_description)
        spinnerStatus = findViewById(R.id.spinner_status)
        btnCreateEvent = findViewById(R.id.btn_create_event)

        // Setup Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.status_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStatus.adapter = adapter
        }

        btnCreateEvent.setOnClickListener {
            createNewEvent()
        }

        // BARU: Set listener untuk Date dan Time Picker
        etDate.setOnClickListener {
            showDatePicker()
        }
        etTime.setOnClickListener {
            showTimePicker()
        }
    }

    // --- FUNGSI DATE AND TIME PICKER (BARU) ---

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            // Format Tanggal ke YYYY-MM-DD
            val formattedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            etDate.setText(formattedDate)
        }, year, month, day)
        dpd.show()
    }

    private fun showTimePicker() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        val tpd = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            // Format Waktu ke HH:MM:SS
            val formattedTime = String.format("%02d:%02d:00", selectedHour, selectedMinute)
            etTime.setText(formattedTime)
        }, hour, minute, true) // 'true' untuk format 24 jam
        tpd.show()
    }

    // Fungsi validasi format Tanggal (DIHAPUS karena picker menjamin format)
    // private fun isValidDateFormat(date: String): Boolean { ... }

    // Fungsi validasi format Waktu (DIHAPUS karena picker menjamin format)
    // private fun isValidTimeFormat(time: String): Boolean { ... }

    private fun createNewEvent() {
        val title = etTitle.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val date = etDate.text.toString().trim()
        val time = etTime.text.toString().trim()
        val capacityStr = etCapacity.text.toString().trim() // Ambil nilai kapasitas
        val description = etDescription.text.toString().trim()
        val status = spinnerStatus.selectedItem.toString()

        // 1. Validasi Wajib Isi (Menambahkan capacityStr.isEmpty() untuk Kapasitas)
        if (title.isEmpty() || location.isEmpty() || date.isEmpty() || time.isEmpty() || capacityStr.isEmpty()) {
            Toast.makeText(this, "Semua field bertanda * wajib diisi.", Toast.LENGTH_LONG).show()
            return
        }

        // Validasi format tanggal dan waktu DIBUANG karena sudah dijamin oleh picker.

        // 2. Konversi kapasitas dan pastikan nilainya valid
        val capacityInt = capacityStr.toIntOrNull()
        if (capacityInt == null || capacityInt <= 0) {
            Toast.makeText(this, "Kapasitas harus berupa angka valid (lebih dari 0).", Toast.LENGTH_LONG).show()
            return
        }


        val newEventData = Event(
            title = title,
            date = date,
            time = time,
            location = location,
            capacity = capacityInt, // Gunakan integer yang sudah divalidasi
            description = description.ifEmpty { null },
            status = status
        )

        // Panggilan API (POST)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.createEvent(newEventData)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == 201) {
                        Toast.makeText(this@NewEventActivity,
                            "✅ Event ${response.body()?.data?.title} Berhasil Dibuat!", Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK) // Memberi sinyal ke MainActivity untuk refresh
                        finish()
                    } else {
                        val message = response.body()?.message ?: "Gagal: Kesalahan data atau server (400/500)."
                        Toast.makeText(this@NewEventActivity,
                            "Gagal: $message", Toast.LENGTH_LONG).show()
                        Log.e("API_CREATE", "Error: ${response.code()}, Message: $message")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NewEventActivity, "Koneksi Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}