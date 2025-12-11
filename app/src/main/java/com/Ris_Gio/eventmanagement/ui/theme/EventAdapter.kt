package com.Ris_Gio.eventmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Ris_Gio.eventmanagement.models.Event
import com.Ris_Gio.eventmanagement.R

class EventAdapter(
    private var eventList: List<Event>,
    private val listener: OnEventActionListener
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    interface OnEventActionListener {
        fun onDeleteClicked(eventId: String)
        fun onEditClicked(event: Event)
        fun onItemClicked(event: Event)
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val id: TextView = itemView.findViewById(R.id.tv_event_id)
        val title: TextView = itemView.findViewById(R.id.tv_event_title)
        val dateTime: TextView = itemView.findViewById(R.id.tv_event_date_time)
        val location: TextView = itemView.findViewById(R.id.tv_event_location)
        val status: TextView = itemView.findViewById(R.id.tv_event_status)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClicked(eventList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]

        // Menggunakan operator Elvis (?: "Nilai Default") untuk Null Safety
        holder.id.text = "ID: ${event.id ?: "N/A"}"
        holder.title.text = event.title ?: "No Title"
        holder.dateTime.text = "Tanggal: ${event.date ?: "N/A"} | ${event.time ?: "N/A"}"
        holder.location.text = "Lokasi: ${event.location ?: "Unknown"}"

        // Menggunakan Safe Call (?.) pada String sebelum memanggil .uppercase()
        // Ini mengatasi error yang Anda lihat di Logcat
        holder.status.text = event.status?.uppercase() ?: "UPCOMING"

    }

    override fun getItemCount(): Int = eventList.size

    fun updateData(newEvents: List<Event>) {
        eventList = newEvents
        notifyDataSetChanged()
    }
}