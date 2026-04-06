/**
 * Callable HTTPS functions — names must match CloudFunctionsRepository.kt:
 *   walletCreditNotify, lowBalanceNotify
 *
 * Region us-central1 matches Firebase Android SDK default for FirebaseFunctions.getInstance().
 */
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

const REGION = "us-central1";

/**
 * Admin credits a player wallet; app sends the target user's FCM token.
 * Only callers with users/{uid}.role === "admin" may invoke.
 */
exports.walletCreditNotify = onCall({region: REGION}, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required.");
  }
  const adminSnap = await admin.firestore().doc(`users/${request.auth.uid}`).get();
  if (!adminSnap.exists || adminSnap.data().role !== "admin") {
    throw new HttpsError("permission-denied", "Only admins can send wallet credit notifications.");
  }

  const {fcmToken, amount} = request.data || {};
  if (!fcmToken || typeof amount !== "number") {
    throw new HttpsError("invalid-argument", "Expected fcmToken (string) and amount (number).");
  }

  const title = "Wallet credited";
  const body = `Your wallet was credited $${amount.toFixed(2)}.`;

  try {
    await admin.messaging().send({
      token: fcmToken,
      notification: {title, body},
      data: {
        type: "wallet_credit",
        amount: String(amount),
      },
    });
    return {ok: true};
  } catch (e) {
    logger.error("walletCreditNotify send failed", e);
    throw new HttpsError("internal", "Failed to send notification.");
  }
});

/**
 * Player balance fell below threshold after registration.
 * Only the signed-in user may notify using their own stored FCM token.
 */
exports.lowBalanceNotify = onCall({region: REGION}, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required.");
  }
  const uid = request.auth.uid;

  const {fcmToken, balance, adminInteracEmail} = request.data || {};
  if (!fcmToken || typeof balance !== "number") {
    throw new HttpsError("invalid-argument", "Expected fcmToken (string) and balance (number).");
  }

  const userSnap = await admin.firestore().doc(`users/${uid}`).get();
  if (!userSnap.exists) {
    throw new HttpsError("not-found", "User profile not found.");
  }
  const stored = userSnap.data().fcmToken || "";
  if (stored !== fcmToken) {
    throw new HttpsError("permission-denied", "FCM token does not match your profile.");
  }

  const interac = typeof adminInteracEmail === "string" && adminInteracEmail.trim()
    ? adminInteracEmail.trim()
    : "your admin";

  const title = "Low balance";
  const body =
    `Your balance is $${balance.toFixed(2)}. Send an Interac e-Transfer to ${interac} and ask for a wallet top-up.`;

  try {
    await admin.messaging().send({
      token: fcmToken,
      notification: {title, body},
      data: {
        type: "low_balance",
        balance: String(balance),
      },
    });
    return {ok: true};
  } catch (e) {
    logger.error("lowBalanceNotify send failed", e);
    throw new HttpsError("internal", "Failed to send notification.");
  }
});
