package com.madhwanikritika.sportsmeet.data.model

import com.google.firebase.Timestamp

data class Registrant(
    val userId: String = "",
    val name: String = "",
    val ticketCode: String = "",
    val registeredAt: Timestamp? = null,
    /** Amount charged for this registration (may differ per player). */
    val pricePaid: Double = 0.0
)
