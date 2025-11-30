package com.Ris_Gio.eventmanagement.networks

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Base URL resmi dari dokumentasi: http://104.248.153.158/event-api/
    // Perhatikan: kita tidak menyertakan 'api.php' di sini.
    private const val BASE_URL = "http://104.248.153.158/event-api/"

    val instance: EventApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Tambahkan converter untuk parsing JSON ke object Kotlin
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Buat implementasi dari EventApiService interface
        retrofit.create(EventApiService::class.java)
    }
}