package com.ganesh.ev.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    }
    
    val authToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[USER_TOKEN] }
    
    val currentUser: Flow<User?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_DATA]?.let { userJson ->
                gson.fromJson(userJson, User::class.java)
            }
        }
    
    val userId: Flow<Long?> = context.dataStore.data
        .map { preferences -> preferences[USER_ID] }
    
    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_TOKEN] = token
        }
    }
    
    suspend fun saveUser(user: User) {
        context.dataStore.edit { preferences ->
            preferences[USER_DATA] = gson.toJson(user)
            preferences[USER_ID] = user.id
        }
    }
    
    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
