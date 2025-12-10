package com.Ris_Gio.eventmanagement.networks


import com.Ris_Gio.eventmanagement.models.ApiResponse
import com.Ris_Gio.eventmanagement.models.Event
import com.Ris_Gio.eventmanagement.models.Statistics
import retrofit2.Response
import retrofit2.http.*

interface EventApiService {

    // 1. GET All Events & Filter Events (READ)
    // Menggunakan parameter Query opsional untuk filter
    @GET("api.php")
    suspend fun getAllEvents(
        @Query("status") status: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<List<Event>>>

    // 2. GET Single Event (READ by ID)
    @GET("api.php")
    suspend fun getEventById(@Query("id") eventId: String): Response<ApiResponse<Event>>

    // 3. POST Create Event (CREATE)
    @POST("api.php")
    suspend fun createEvent(@Body event: Event): Response<ApiResponse<Event>>

    // 4. PUT Update Event (UPDATE)
    // ID event di-pass sebagai Query parameter, data baru di-pass sebagai Body
    @PUT("api.php")
    suspend fun updateEvent(
        @Query("id") eventId: String,
        @Body event: Event
    ): Response<ApiResponse<Event>>

    // 5. DELETE Event (DELETE)
    @DELETE("api.php")
    suspend fun deleteEvent(@Query("id") eventId: String): Response<ApiResponse<Any>>

    // 6. GET Statistics
    @GET("api.php")
    suspend fun getStatistics(@Query("stats") stats: Int = 1): Response<ApiResponse<Statistics>>
}

