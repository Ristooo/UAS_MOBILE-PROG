package com.Ris_Gio.eventmanagement

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.Ris_Gio.eventmanagement.models.Event
import com.Ris_Gio.eventmanagement.R
import android.widget.Toast // Tambahkan ini jika belum ada

class EventDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // PERBAIKAN TEMA: Panggil setTheme secara eksplisit untuk mengatasi IllegalStateException
        setTheme(R.style.Theme_EventManagementApp)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        // Ambil objek Event dari Intent
        val event = intent.getSerializableExtra("EXTRA_EVENT_DETAIL") as? Event

        if (event == null) {
            Toast.makeText(this, "Gagal memuat detail event.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Menggunakan operator Elvis (?:) untuk memberikan nilai default jika null
        supportActionBar?.title = event.title ?: "Detail Event" // Memperbaiki baris 35
        displayEventDetails(event)
    }

    private fun displayEventDetails(event: Event) {
        findViewById<TextView>(R.id.tv_detail_id).text = "ID: ${event.id ?: "N/A"}"
        findViewById<TextView>(R.id.tv_detail_location).text = event.location ?: "Lokasi Tidak Ditemukan"
        findViewById<TextView>(R.id.tv_detail_date_time).text = "${event.date ?: "N/A"} | ${event.time ?: "N/A"}"
        findViewById<TextView>(R.id.tv_detail_status).text = event.status?.uppercase() ?: "STATUS TIDAK DIKETAHUI"
        findViewById<TextView>(R.id.tv_detail_capacity).text = "${event.capacity ?: "Tidak Terbatas"} Orang"
        findViewById<TextView>(R.id.tv_detail_description).text = event.description ?: "Tidak ada deskripsi."
    }
}