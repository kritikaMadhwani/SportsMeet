package com.madhwanikritika.sportsmeet.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.madhwanikritika.sportsmeet.data.firestore.FirestorePaths
import com.madhwanikritika.sportsmeet.data.firestore.toUser
import com.madhwanikritika.sportsmeet.data.local.SessionPreferences
import com.madhwanikritika.sportsmeet.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionPreferences: SessionPreferences
) {

    val currentAuthUid: String?
        get() = auth.currentUser?.uid

    fun authStateFlow(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser != null)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(IllegalStateException("No uid"))
            val user = firestore.collection(FirestorePaths.USERS).document(uid).get().await().toUser()
                ?: return Result.failure(IllegalStateException("User profile missing"))
            sessionPreferences.saveSession(uid, user.email)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, name: String, phone: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(IllegalStateException("No uid"))
            val user = User(
                userId = uid,
                name = name,
                email = email,
                phone = phone,
                role = "user",
                walletBalance = 0.0,
                fcmToken = "",
                createdAt = Timestamp.now()
            )
            firestore.collection(FirestorePaths.USERS).document(uid).set(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "role" to "user",
                    "walletBalance" to 0.0,
                    "fcmToken" to "",
                    "createdAt" to Timestamp.now()
                )
            ).await()
            sessionPreferences.saveSession(uid, email)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserById(uid: String): Flow<User?> = callbackFlow {
        var registration: ListenerRegistration? = null
        registration = firestore.collection(FirestorePaths.USERS).document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snap?.toUser())
            }
        awaitClose { registration?.remove() }
    }

    suspend fun updateFcmToken(uid: String, token: String) {
        firestore.collection(FirestorePaths.USERS).document(uid)
            .update("fcmToken", token)
            .await()
    }

    suspend fun signOut() {
        auth.signOut()
        sessionPreferences.clear()
    }

    suspend fun refreshUser(uid: String): User? {
        return firestore.collection(FirestorePaths.USERS).document(uid).get().await().toUser()
    }
}
