package com.madhwanikritika.sportsmeet.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.madhwanikritika.sportsmeet.data.model.AppConfig
import com.madhwanikritika.sportsmeet.data.model.Event
import com.madhwanikritika.sportsmeet.data.model.Registrant
import com.madhwanikritika.sportsmeet.data.model.Ticket
import com.madhwanikritika.sportsmeet.data.model.User
import com.madhwanikritika.sportsmeet.data.model.WalletTransaction

fun DocumentSnapshot.toUser(): User? {
    if (!exists()) return null
    return User(
        userId = id,
        name = getString("name").orEmpty(),
        email = getString("email").orEmpty(),
        phone = getString("phone").orEmpty(),
        role = getString("role").orEmpty(),
        walletBalance = getDouble("walletBalance") ?: 0.0,
        fcmToken = getString("fcmToken").orEmpty(),
        createdAt = getTimestamp("createdAt")
    )
}

fun DocumentSnapshot.toEvent(registrants: List<Registrant> = emptyList()): Event? {
    if (!exists()) return null
    return Event(
        eventId = id,
        sport = getString("sport").orEmpty(),
        icon = getString("icon").orEmpty(),
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        dateMillis = getLong("dateMillis") ?: 0L,
        time = getString("time").orEmpty(),
        location = getString("location").orEmpty(),
        totalSeats = getLong("totalSeats")?.toInt() ?: 0,
        filledSeats = getLong("filledSeats")?.toInt() ?: 0,
        price = getDouble("price") ?: 0.0,
        createdBy = getString("createdBy").orEmpty(),
        registrants = registrants,
        createdAt = getTimestamp("createdAt")
    )
}

fun DocumentSnapshot.toWalletTransaction(): WalletTransaction? {
    if (!exists()) return null
    return WalletTransaction(
        transactionId = id,
        type = getString("type").orEmpty(),
        amount = getDouble("amount") ?: 0.0,
        description = getString("description").orEmpty(),
        eventId = getString("eventId"),
        creditedBy = getString("creditedBy"),
        timestamp = getTimestamp("timestamp")
    )
}

fun DocumentSnapshot.toTicket(): Ticket? {
    if (!exists()) return null
    return Ticket(
        ticketId = id,
        eventId = getString("eventId").orEmpty(),
        eventTitle = getString("eventTitle").orEmpty(),
        sport = getString("sport").orEmpty(),
        date = getString("date").orEmpty(),
        time = getString("time").orEmpty(),
        location = getString("location").orEmpty(),
        price = getDouble("price") ?: 0.0,
        ticketCode = getString("ticketCode").orEmpty(),
        timestamp = getTimestamp("timestamp")
    )
}

fun DocumentSnapshot.toRegistrant(): Registrant? {
    if (!exists()) return null
    return Registrant(
        userId = getString("userId").orEmpty().ifBlank { id },
        name = getString("name").orEmpty(),
        ticketCode = getString("ticketCode").orEmpty(),
        registeredAt = getTimestamp("registeredAt"),
        pricePaid = getDouble("pricePaid") ?: 0.0
    )
}

fun DocumentSnapshot.toAppConfig(): AppConfig {
    return AppConfig(
        lowBalanceThreshold = getDouble("lowBalanceThreshold") ?: 20.0,
        adminEmail = getString("adminEmail").orEmpty(),
        adminInteracEmail = getString("adminInteracEmail").orEmpty()
    )
}

fun formatTicketDate(millis: Long): String {
    val fmt = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.getDefault())
    return fmt.format(java.util.Date(millis))
}

fun nowTimestamp(): Timestamp = Timestamp.now()
