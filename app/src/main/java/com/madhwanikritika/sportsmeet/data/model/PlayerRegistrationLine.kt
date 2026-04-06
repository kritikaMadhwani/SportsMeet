package com.madhwanikritika.sportsmeet.data.model

/**
 * Admin-defined registration with a specific charge (deducted from the player's wallet).
 */
data class PlayerRegistrationLine(
    val userId: String,
    val userName: String,
    val price: Double
)
