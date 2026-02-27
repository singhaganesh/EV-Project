package com.ganesh.ev.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ganesh.ev.data.model.User
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private val gson = Gson()

    companion object {
        val USER_TOKEN = stringPreferencesKey("user_token")
        val USER_DATA = stringPreferencesKey("user_data")
        val USER_ID = longPreferencesKey("user_id")
        val SHOULD_SHOW_ONBOARDING = booleanPreferencesKey("should_show_onboarding")
    }

    val shouldShowOnboarding: Flow<Boolean> =
            context.dataStore.data.map { preferences ->
                preferences[SHOULD_SHOW_ONBOARDING] ?: true
            }

    val authToken: Flow<String?> =
            context.dataStore.data.map { preferences -> preferences[USER_TOKEN] }

    val currentUser: Flow<User?> =
            context.dataStore.data.map { preferences ->
                preferences[USER_DATA]?.let { userJson ->
                    gson.fromJson(userJson, User::class.java)
                }
            }

    val userId: Flow<Long?> = context.dataStore.data.map { preferences -> preferences[USER_ID] }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences -> preferences[USER_TOKEN] = token }
    }

    suspend fun saveUser(user: User) {
        context.dataStore.edit { preferences ->
            preferences[USER_DATA] = gson.toJson(user)
            preferences[USER_ID] = user.id
        }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences -> preferences[SHOULD_SHOW_ONBOARDING] = false }
    }

    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
            // We usually want to keep the onboarding flag false if they already did it once
            // but for major logouts/resets, we might want to keep it or reset it.
            // Let's reset everything but onboarding - actually, keep it simple for now:
            // preferences.clear() clears EVERYTHING including onboarding.
        }
    }
}
