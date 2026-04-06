package com.madhwanikritika.sportsmeet.data.model

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val walletBalance: Double = 0.0,
    val fcmToken: String = "",
    val createdAt: Timestamp? = null
)
