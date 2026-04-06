package com.madhwanikritika.sportsmeet.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madhwanikritika.sportsmeet.data.firestore.FirestorePaths
import com.madhwanikritika.sportsmeet.data.firestore.toUser
import com.madhwanikritika.sportsmeet.data.firestore.toWalletTransaction
import com.madhwanikritika.sportsmeet.data.model.User
import com.madhwanikritika.sportsmeet.data.model.WalletTransaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Firestore security rules (mirror of firestore.rules at project root):
 *
 * users/{uid}:
 *   - read: request.auth != null && (request.auth.uid == uid || caller.role == "admin")
 *   - write: request.auth != null && (request.auth.uid == uid || caller.role == "admin")
 *
 * users/{uid}/walletTransactions/{txId}:
 *   - read/write: owner or admin
 *
 * events:
 *   - read: authenticated
 *   - write: admin for create/delete; filledSeats +1 self-registration rule for updates
 *
 * users/{uid}/tickets:
 *   - read/write: uid matches auth
 *
 * config/app:
 *   - read: authenticated
 *   - write: admin
 */
@Singleton
class WalletRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudFunctionsRepository: CloudFunctionsRepository
) {

    fun getWalletBalance(uid: String): Flow<Double> = callbackFlow {
        val reg = firestore.collection(FirestorePaths.USERS).document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                trySend(snap?.getDouble("walletBalance") ?: 0.0)
            }
        awaitClose { reg.remove() }
    }

    fun getTransactions(uid: String): Flow<List<WalletTransaction>> = callbackFlow {
        val reg = firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.WALLET_TRANSACTIONS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { qs, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                trySend(qs?.documents?.mapNotNull { it.toWalletTransaction() }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun getAllUsersWithBalances(): Flow<List<User>> = callbackFlow {
        val reg = firestore.collection(FirestorePaths.USERS)
            .whereEqualTo("role", "user")
            .addSnapshotListener { qs, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                trySend(qs?.documents?.mapNotNull { it.toUser() }.orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun creditWallet(
        adminUid: String,
        targetUid: String,
        amount: Double,
        description: String
    ): Result<Unit> {
        if (amount <= 0) return Result.failure(IllegalArgumentException("Amount must be positive"))
        return try {
            val adminRef = firestore.collection(FirestorePaths.USERS).document(adminUid)
            val targetRef = firestore.collection(FirestorePaths.USERS).document(targetUid)
            val txId = targetRef.collection(FirestorePaths.WALLET_TRANSACTIONS).document().id
            val txRef = targetRef.collection(FirestorePaths.WALLET_TRANSACTIONS).document(txId)

            firestore.runTransaction { tx ->
                val adminSnap = tx.get(adminRef)
                if (adminSnap.getString("role") != "admin") {
                    throw SecurityException("Only admins can credit wallets")
                }
                val targetSnap = tx.get(targetRef)
                val current = targetSnap.getDouble("walletBalance") ?: 0.0
                val newBalance = current + amount
                tx.update(targetRef, mapOf("walletBalance" to newBalance))
                tx.set(
                    txRef,
                    mapOf(
                        "type" to "credit",
                        "amount" to amount,
                        "description" to description.ifBlank { "Top-up by admin" },
                        "eventId" to null,
                        "creditedBy" to adminUid,
                        "timestamp" to Timestamp.now()
                    )
                )
            }.await()

            val fcm = firestore.collection(FirestorePaths.USERS).document(targetUid).get().await()
                .getString("fcmToken").orEmpty()
            cloudFunctionsRepository.notifyWalletCredit(fcm, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
