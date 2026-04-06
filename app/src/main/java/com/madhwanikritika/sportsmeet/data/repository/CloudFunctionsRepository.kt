package com.madhwanikritika.sportsmeet.data.repository

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls **Firebase Callable (HTTPS)** Cloud Functions from the app — no custom REST API.
 *
 * Deploy functions with matching names, or change [FN_WALLET_CREDIT] / [FN_LOW_BALANCE] here
 * to match your `functions` folder (e.g. `exports.walletCreditNotify = functions.https.onCall(...)`).
 *
 * **Region:** If your functions are not in the default region, use
 * `FirebaseFunctions.getInstance("europe-west1")` in [com.madhwanikritika.sportsmeet.di.FirebaseModule].
 *
 * **Emulator (optional):** `FirebaseFunctions.getInstance().useEmulator("10.0.2.2", 5001)` in debug.
 */
@Singleton
class CloudFunctionsRepository @Inject constructor(
    private val functions: FirebaseFunctions
) {

    suspend fun notifyWalletCredit(fcmToken: String, amount: Double) {
        if (fcmToken.isBlank()) {
            Log.w(TAG, "notifyWalletCredit: empty FCM token")
            return
        }
        runCatching {
            val data = hashMapOf<String, Any>(
                "fcmToken" to fcmToken,
                "amount" to amount
            )
            functions.getHttpsCallable(FN_WALLET_CREDIT).call(data).await()
        }.onFailure { e ->
            Log.w(TAG, "Callable '$FN_WALLET_CREDIT' failed (deploy function or rename constant): ${e.message}")
        }
    }

    suspend fun notifyLowBalance(
        fcmToken: String,
        balance: Double,
        adminInteracEmail: String
    ) {
        if (fcmToken.isBlank()) {
            Log.w(TAG, "notifyLowBalance: empty FCM token")
            return
        }
        runCatching {
            val data = hashMapOf<String, Any>(
                "fcmToken" to fcmToken,
                "balance" to balance,
                "adminInteracEmail" to adminInteracEmail
            )
            functions.getHttpsCallable(FN_LOW_BALANCE).call(data).await()
        }.onFailure { e ->
            Log.w(TAG, "Callable '$FN_LOW_BALANCE' failed (deploy function or rename constant): ${e.message}")
        }
    }

    companion object {
        private const val TAG = "CloudFunctions"

        /** Rename to match your deployed callable name. */
        private const val FN_WALLET_CREDIT = "walletCreditNotify"

        /** Rename to match your deployed callable name. */
        private const val FN_LOW_BALANCE = "lowBalanceNotify"
    }
}
