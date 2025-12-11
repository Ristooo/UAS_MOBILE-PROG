package com.Ris_Gio.eventmanagement

import android.os.Bundle
import android.util.Log
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
import java.util.Calendar // Import yang diperlukan

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

        // AKTIFKAN TOMBOL UP/KEMBALI KE PARENT ACTIVITY (BARU)
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

        // Panggil fungsi setup picker baru
        setupDateTimePickers()

        btnCreateEvent.setOnClickListener {
            createNewEvent()
        }
    }

    private fun setupDateTimePickers() {
        // 1. Setup Date Picker
        etDate.apply {
            isFocusable = false // Mencegah keyboard muncul
            isClickable = true
            setOnClickListener {
                showDatePickerDialog()
            }
        }

        // 2. Setup Time Picker
        etTime.apply {
            isFocusable = false // Mencegah keyboard muncul
            isClickable = true
            setOnClickListener {
                showTimePickerDialog()
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format tanggal menjadi YYYY-MM-DD
                val formattedDate = String.format(
                    "%d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1, // Bulan dimulai dari 0
                    selectedDay
                )
                etDate.setText(formattedDate)
            },
            year,
            month,
            day
        ).show()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        android.app.TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                // Format waktu menjadi HH:MM:00
                val formattedTime = String.format(
                    "%02d:%02d:00",
                    selectedHour,
                    selectedMinute
                )
                etTime.setText(formattedTime)
            },
            hour,
            minute,
            true // true untuk format 24 jam
        ).show()
    }


    // Fungsi validasi format Tanggal (sudah tidak terlalu penting karena dipilih, tapi dipertahankan untuk keamanan)
    private fun isValidDateFormat(date: String): Boolean {
        return date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
    }

    // Fungsi validasi format Waktu (sudah tidak terlalu penting karena dipilih, tapi dipertahankan untuk keamanan)
    private fun isValidTimeFormat(time: String): Boolean {
        // Menerima HH:MM:SS atau HH:MM
        return time.matches(Regex("^\\d{2}:\\d{2}:\\d{2}$")) || time.matches(Regex("^\\d{2}:\\d{2}$"))
    }

    private fun createNewEvent() {
        val title = etTitle.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val date = etDate.text.toString().trim()
        val time = etTime.text.toString().trim()
        val capacityStr = etCapacity.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val status = spinnerStatus.selectedItem.toString()

        // 1. Validasi Wajib Isi
        if (title.isEmpty() || location.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Semua field bertanda * wajib diisi.", Toast.LENGTH_LONG).show()
            return
        }

        // 2. Validasi Format API (Dipertahankan sebagai safety net)
        if (!isValidDateFormat(date)) {
            Toast.makeText(this, "Gagal: Format Tanggal harus YYYY-MM-DD.", Toast.LENGTH_LONG).show()
            return
        }

        if (!isValidTimeFormat(time)) {
            Toast.makeText(this, "Gagal: Format Waktu harus HH:MM:SS atau HH:MM.", Toast.LENGTH_LONG).show()
            return
        }

        val newEventData = Event(
            title = title,
            date = date,
            time = time,
            location = location,
            capacity = capacityStr.toIntOrNull(),
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