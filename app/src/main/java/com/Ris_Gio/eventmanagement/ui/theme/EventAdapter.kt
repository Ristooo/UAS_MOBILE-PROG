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
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Pastikan ID di sini sesuai dengan item_event.xml
        val title: TextView = itemView.findViewById(R.id.tv_event_title)
        val dateTime: TextView = itemView.findViewById(R.id.tv_event_date_time)
        val location: TextView = itemView.findViewById(R.id.tv_event_location)
        val status: TextView = itemView.findViewById(R.id.tv_event_status)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]

        holder.title.text = event.title
        holder.dateTime.text = "Tanggal: ${event.date} | ${event.time}"
        holder.location.text = "Lokasi: ${event.location}"
        holder.status.text = event.status.uppercase()

        // Setup listener di ViewHolder
        holder.btnEdit.setOnClickListener {
            listener.onEditClicked(event)
        }

        holder.btnDelete.setOnClickListener {
            event.id?.let { id ->
                listener.onDeleteClicked(id)
            }
        }
    }

    override fun getItemCount(): Int = eventList.size

    fun updateData(newEvents: List<Event>) {
        eventList = newEvents
        notifyDataSetChanged()
    }
}