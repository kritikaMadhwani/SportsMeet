package com.madhwanikritika.sportsmeet.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.madhwanikritika.sportsmeet.data.firestore.FirestorePaths
import com.madhwanikritika.sportsmeet.data.firestore.toAppConfig
import com.madhwanikritika.sportsmeet.data.model.AppConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val ref
        get() = firestore.collection(FirestorePaths.CONFIG).document(FirestorePaths.APP_CONFIG_DOC)

    fun getAppConfig(): Flow<AppConfig> = callbackFlow {
        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            if (snap != null && snap.exists()) {
                trySend(snap.toAppConfig())
            } else {
                trySend(
                    AppConfig(
                        lowBalanceThreshold = 20.0,
                        adminEmail = "",
                        adminInteracEmail = ""
                    )
                )
            }
        }
        awaitClose { reg.remove() }
    }

    suspend fun updateLowBalanceThreshold(amount: Double, adminUid: String) {
        assertAdmin(adminUid)
        ref.update("lowBalanceThreshold", amount).await()
    }

    suspend fun updateAdminInteracEmail(email: String, adminUid: String) {
        assertAdmin(adminUid)
        ref.update("adminInteracEmail", email).await()
    }

    suspend fun updateAdminEmail(email: String, adminUid: String) {
        assertAdmin(adminUid)
        ref.update("adminEmail", email).await()
    }

    suspend fun ensureDefaultConfig() {
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(
                mapOf(
                    "lowBalanceThreshold" to 20.0,
                    "adminEmail" to "",
                    "adminInteracEmail" to ""
                )
            ).await()
        }
    }

    private suspend fun assertAdmin(adminUid: String) {
        val u = firestore.collection(FirestorePaths.USERS).document(adminUid).get().await()
        if (u.getString("role") != "admin") throw SecurityException("Admin only")
    }
}
