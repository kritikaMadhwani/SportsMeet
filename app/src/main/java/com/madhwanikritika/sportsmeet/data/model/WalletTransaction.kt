package com.madhwanikritika.sportsmeet.data.model

import com.google.firebase.Timestamp

data class WalletTransaction(
    val transactionId: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val eventId: String? = null,
    val creditedBy: String? = null,
    val timestamp: Timestamp? = null
)
