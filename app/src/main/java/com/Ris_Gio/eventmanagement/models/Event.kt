package com.Ris_Gio.eventmanagement.models

import com.google.gson.annotations.SerializedName

data class Event(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("title")
    val title: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("time")
    val time: String,

    @SerializedName("location")
    val location: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("capacity")
    val capacity: Int? = null,

    @SerializedName("status")
    val status: String
)