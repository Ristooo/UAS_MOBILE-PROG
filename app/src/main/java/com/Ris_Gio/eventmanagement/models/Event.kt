package com.Ris_Gio.eventmanagement.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Data class Event harus mengimplementasikan Serializable
data class Event(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("capacity") val capacity: Int? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("status") val status: String? = null
) : Serializable // <--- INI KRITIS UNTUK MEMPERBAIKI ERROR PUTEXTRA