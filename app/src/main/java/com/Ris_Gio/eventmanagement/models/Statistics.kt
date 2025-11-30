package com.Ris_Gio.eventmanagement.models

import com.google.gson.annotations.SerializedName

data class Statistics(
    @SerializedName("total")
    val total: String,

    @SerializedName("upcoming")
    val upcoming: String,

    @SerializedName("ongoing")
    val ongoing: String,

    @SerializedName("completed")
    val completed: String,

    @SeralizedName("cancelled")
    val cancelled: String
)