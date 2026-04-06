package com.madhwanikritika.sportsmeet.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madhwanikritika.sportsmeet.data.firestore.FirestorePaths
import com.madhwanikritika.sportsmeet.data.firestore.formatTicketDate
import com.madhwanikritika.sportsmeet.data.firestore.toEvent
import com.madhwanikritika.sportsmeet.data.firestore.toTicket
import com.madhwanikritika.sportsmeet.data.model.Event
import com.madhwanikritika.sportsmeet.data.model.PlayerRegistrationLine
import com.madhwanikritika.sportsmeet.data.model.Ticket
import com.madhwanikritika.sportsmeet.domain.InsufficientBalanceException
import com.madhwanikritika.sportsmeet.domain.SoldOutException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val configRepository: ConfigRepository,
    private val cloudFunctionsRepository: CloudFunctionsRepository
) {

    fun getAllEvents(): Flow<List<Event>> = callbackFlow {
        val reg = firestore.collection(FirestorePaths.EVENTS)
            .orderBy("dateMillis", Query.Direction.ASCENDING)
            .addSnapshotListener { qs, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = qs?.documents?.mapNotNull { it.toEvent() }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun createEvent(event: Event, adminUid: String) {
        val ref = firestore.collection(FirestorePaths.EVENTS).document()
        val data = mapOf(
            "sport" to event.sport,
            "icon" to event.icon,
            "title" to event.title,
            "description" to event.description,
            "dateMillis" to event.dateMillis,
            "time" to event.time,
            "location" to event.location,
            "totalSeats" to event.totalSeats,
            "filledSeats" to 0,
            "price" to event.price,
            "createdBy" to adminUid,
            "createdAt" to Timestamp.now()
        )
        ref.set(data).await()
    }

    /**
     * Creates the event then registers each player with their own price (wallet debits).
     * If a registration fails after the event exists, returns failure with details (partial registrations may remain).
     */
    suspend fun createEventWithRegistrations(
        event: Event,
        adminUid: String,
        initialRegistrations: List<PlayerRegistrationLine>
    ): Result<String> {
        return try {
            if (initialRegistrations.size > event.totalSeats) {
                return Result.failure(
                    IllegalArgumentException("More players than available seats (${event.totalSeats}).")
                )
            }
            val uids = initialRegistrations.map { it.userId }
            if (uids.distinct().size != uids.size) {
                return Result.failure(IllegalArgumentException("The same player appears more than once."))
            }
            val ref = firestore.collection(FirestorePaths.EVENTS).document()
            val eventId = ref.id
            val data = mapOf(
                "sport" to event.sport,
                "icon" to event.icon,
                "title" to event.title,
                "description" to event.description,
                "dateMillis" to event.dateMillis,
                "time" to event.time,
                "location" to event.location,
                "totalSeats" to event.totalSeats,
                "filledSeats" to 0,
                "price" to event.price,
                "createdBy" to adminUid,
                "createdAt" to Timestamp.now()
            )
            ref.set(data).await()
            for (line in initialRegistrations) {
                val r = registerForEvent(
                    eventId = eventId,
                    userId = line.userId,
                    userName = line.userName,
                    priceOverride = line.price
                )
                if (r.isFailure) {
                    return Result.failure(
                        Exception(
                            "Event was created but registration failed for ${line.userName}: " +
                                "${r.exceptionOrNull()?.message}"
                        )
                    )
                }
            }
            Result.success(eventId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * @param priceOverride if set, this amount is charged instead of the event’s default [Event.price].
     */
    suspend fun registerForEvent(
        eventId: String,
        userId: String,
        userName: String,
        priceOverride: Double? = null
    ): Result<String> {
        return try {
            val configSnap = firestore.collection(FirestorePaths.CONFIG)
                .document(FirestorePaths.APP_CONFIG_DOC).get().await()
            val adminInterac = configSnap.getString("adminInteracEmail").orEmpty()
            val lowThreshold = configSnap.getDouble("lowBalanceThreshold") ?: 20.0

            val eventRef = firestore.collection(FirestorePaths.EVENTS).document(eventId)
            val userRef = firestore.collection(FirestorePaths.USERS).document(userId)
            val ticketId = firestore.collection(FirestorePaths.USERS).document(userId)
                .collection(FirestorePaths.TICKETS).document().id
            val ticketRef = firestore.collection(FirestorePaths.USERS).document(userId)
                .collection(FirestorePaths.TICKETS).document(ticketId)
            val walletTxId = userRef.collection(FirestorePaths.WALLET_TRANSACTIONS).document().id
            val walletTxRef = userRef.collection(FirestorePaths.WALLET_TRANSACTIONS).document(walletTxId)
            val registrantRef = eventRef.collection(FirestorePaths.REGISTRANTS).document(userId)

            val ticketCode = UUID.randomUUID().toString().take(8).uppercase()

            val newBalance = firestore.runTransaction { tx ->
                val eventSnap = tx.get(eventRef)
                val userSnap = tx.get(userRef)
                if (!eventSnap.exists()) throw IllegalStateException("Event not found")
                if (!userSnap.exists()) throw IllegalStateException("User not found")
                if (tx.get(registrantRef).exists()) {
                    throw IllegalStateException("This player is already registered for this event.")
                }

                val filled = eventSnap.getLong("filledSeats")?.toInt() ?: 0
                val total = eventSnap.getLong("totalSeats")?.toInt() ?: 0
                val price = priceOverride ?: (eventSnap.getDouble("price") ?: 0.0)
                val balance = userSnap.getDouble("walletBalance") ?: 0.0

                if (filled >= total) throw SoldOutException()
                if (balance < price) {
                    throw InsufficientBalanceException(
                        "Insufficient balance. Please transfer funds to the admin via Interac and ask them to top up your wallet."
                    )
                }

                val after = balance - price
                tx.update(userRef, mapOf("walletBalance" to after))
                tx.set(
                    walletTxRef,
                    mapOf(
                        "type" to "debit",
                        "amount" to price,
                        "description" to (eventSnap.getString("title") ?: "Event registration"),
                        "eventId" to eventId,
                        "creditedBy" to null,
                        "timestamp" to Timestamp.now()
                    )
                )
                tx.update(eventRef, mapOf("filledSeats" to filled + 1))
                tx.set(
                    registrantRef,
                    mapOf(
                        "userId" to userId,
                        "name" to userName,
                        "ticketCode" to ticketCode,
                        "registeredAt" to Timestamp.now(),
                        "pricePaid" to price
                    )
                )
                val dateStr = formatTicketDate(eventSnap.getLong("dateMillis") ?: 0L)
                tx.set(
                    ticketRef,
                    mapOf(
                        "eventId" to eventId,
                        "eventTitle" to eventSnap.getString("title").orEmpty(),
                        "sport" to eventSnap.getString("sport").orEmpty(),
                        "date" to dateStr,
                        "time" to eventSnap.getString("time").orEmpty(),
                        "location" to eventSnap.getString("location").orEmpty(),
                        "price" to price,
                        "ticketCode" to ticketCode,
                        "timestamp" to Timestamp.now()
                    )
                )
                after
            }.await()

            if (newBalance < lowThreshold) {
                val fcm = userRef.get().await().getString("fcmToken").orEmpty()
                val interac = adminInterac.ifBlank { "admin@example.com" }
                cloudFunctionsRepository.notifyLowBalance(
                    fcmToken = fcm,
                    balance = newBalance,
                    adminInteracEmail = interac
                )
            }

            Result.success(ticketCode)
        } catch (e: InsufficientBalanceException) {
            Result.failure(e)
        } catch (e: SoldOutException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserTickets(uid: String): Flow<List<Ticket>> = callbackFlow {
        val reg = firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.TICKETS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { qs, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                trySend(qs?.documents?.mapNotNull { it.toTicket() }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun isUserRegistered(eventId: String, uid: String): Boolean {
        val snap = firestore.collection(FirestorePaths.EVENTS).document(eventId)
            .collection(FirestorePaths.REGISTRANTS).document(uid).get().await()
        return snap.exists()
    }

    suspend fun getEvent(eventId: String): Event? {
        val snap = firestore.collection(FirestorePaths.EVENTS).document(eventId).get().await()
        return snap.toEvent()
    }
}
