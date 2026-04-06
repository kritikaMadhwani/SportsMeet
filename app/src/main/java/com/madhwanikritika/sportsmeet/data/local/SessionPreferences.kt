package com.madhwanikritika.sportsmeet.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.sessionDataStore

    companion object {
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_EMAIL = stringPreferencesKey("user_email")
    }

    val sessionFlow: Flow<SessionSnapshot> = dataStore.data.map { prefs ->
        SessionSnapshot(
            userId = prefs[USER_ID],
            email = prefs[USER_EMAIL]
        )
    }

    suspend fun saveSession(userId: String, email: String) {
        dataStore.edit { prefs ->
            prefs[USER_ID] = userId
            prefs[USER_EMAIL] = email
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

data class SessionSnapshot(
    val userId: String?,
    val email: String?
)
