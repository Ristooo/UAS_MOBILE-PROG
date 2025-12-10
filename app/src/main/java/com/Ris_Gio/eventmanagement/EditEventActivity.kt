package com.Ris_Gio.eventmanagement

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

class EditEventActivity : AppCompatActivity() {
    private lateinit var etTitle: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var etCapacity: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerStatus: Spinner
    private lateinit var btnSaveChanges: Button
    private lateinit var progressBar: ProgressBar

    private var eventId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_event)
        eventId = intent.getStringExtra("EXTRA_EVENT_ID")

        if (eventId == null) {
            Toast.makeText(this, "Event ID tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        initViews()
        fetchEventDetails(eventId!!)
        btnSaveChanges.setOnClickListener {
            executeUpdateEvent(eventId!!)
        }
    }

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

        ArrayAdapter.createFromResource(
            this,
            R.array.status_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStatus.adapter = adapter
        }
    }

    private fun fetchEventDetails(id: String) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getEventById(id)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.data != null) {
                        fillFormWithData(response.body()!!.data!!)
                    } else {
                        Toast.makeText(
                            this@EditEventActivity,
                            "Gagal memuat detail event.",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@EditEventActivity,
                        "Koneksi Gagal: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun fillFormWithData(event: Event) {
        val statusOptions = resources.getStringArray(R.array.status_options)
        val statusIndex = statusOptions.indexOf(event.status)

        etTitle.setText(event.title)
        etLocation.setText(event.location)
        etDate.setText(event.date)
        etTime.setText(event.time)
        etCapacity.setText(event.capacity?.toString() ?: "")
        etDescription.setText(event.description ?: "")
        if (statusIndex >= 0) {
            spinnerStatus.setSelection(statusIndex)
        }
    }

    private fun executeUpdateEvent(id: String) {
        val title = etTitle.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val date = etDate.text.toString().trim()
        val time = etTime.text.toString().trim()
        val capacityStr = etCapacity.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val status = spinnerStatus.selectedItem.toString()

        if (title.isEmpty() || location.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Semua field bertanda * wajib diisi.", Toast.LENGTH_LONG).show()
            return
        }
        val updatedEventData = Event(
            id = id,
            title = title,
            date = date,
            time = time,
            location = location,
            capacity = capacityStr.toIntOrNull(),
            description = description.ifEmpty { null },
            status = status
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.updateEvent(id, updatedEventData)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == 200) {
                        Toast.makeText(
                            this@EditEventActivity,
                            "✅ Event Berhasil Diperbarui!", Toast.LENGTH_LONG
                        ).show()

                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val message =
                            response.body()?.message ?: "Gagal memperbarui event (400/500)."
                        Toast.makeText(
                            this@EditEventActivity,
                            "Gagal: $message", Toast.LENGTH_LONG
                        ).show()
                        Log.e("API_UPDATE", "Error: ${response.code()}, Message: $message")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@EditEventActivity,
                        "Koneksi Gagal: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}