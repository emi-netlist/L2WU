package com.emi.l2wu.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Top-level delegate to create the DataStore instance
val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/*  The DataStore Manager

    This class handles the actual reading and writing to the DataStore file on the disk.
    It exposes the boolean as a Flow<Boolean>.
 */
class SettingsManager(private val context: Context) {

    companion object {
        // key for the boolean value
        val IS_SERVICE_STARTED = booleanPreferencesKey("is_service_started")
    }

    // Read the boolean value (defaults to false if not found)
    val isServiceStarted: Flow<Boolean> = context.datastore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_SERVICE_STARTED] ?: false
        }

    // Write the boolean value
    suspend fun setIsServiceStarted(isEnabled: Boolean) {
        context.datastore.edit { preferences ->
            preferences[IS_SERVICE_STARTED] = isEnabled
        }
    }
}