package com.madhwanikritika.sportsmeet.data.model

import com.google.firebase.Timestamp

data class Event(
    val eventId: String = "",
    val sport: String = "",
    val icon: String = "",
    val title: String = "",
    val description: String = "",
    val dateMillis: Long = 0L,
    val time: String = "",
    val location: String = "",
    val totalSeats: Int = 0,
    val filledSeats: Int = 0,
    val price: Double = 0.0,
    val createdBy: String = "",
    val registrants: List<Registrant> = emptyList(),
    val createdAt: Timestamp? = null
)
