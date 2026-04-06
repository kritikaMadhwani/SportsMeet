package com.madhwanikritika.sportsmeet.data.model

import com.google.firebase.Timestamp

data class Ticket(
    val ticketId: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val sport: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val price: Double = 0.0,
    val ticketCode: String = "",
    val timestamp: Timestamp? = null
)
