package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.TeacherSession
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "elimutrack_session")

class SessionPreferences(private val context: Context) {
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val sessionAdapter = moshi.adapter(TeacherSession::class.java)

  private object Keys {
    val SESSION_JSON = stringPreferencesKey("session_json")
    val BACKEND_URL = stringPreferencesKey("backend_url")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
  }

  val sessionFlow: Flow<TeacherSession?> = context.dataStore.data.map { prefs ->
    prefs[Keys.SESSION_JSON]?.let { json ->
      runCatching { sessionAdapter.fromJson(json) }.getOrNull()
    }
  }

  val backendUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[Keys.BACKEND_URL] ?: TeacherSession.DEFAULT_BACKEND_URL
  }

  val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[Keys.DARK_MODE] ?: true
  }

  val lastSyncedAtFlow: Flow<Long?> = context.dataStore.data.map { prefs ->
    prefs[Keys.LAST_SYNCED_AT]
  }

  suspend fun saveSession(session: TeacherSession) {
    context.dataStore.edit { prefs ->
      prefs[Keys.SESSION_JSON] = sessionAdapter.toJson(session)
      prefs[Keys.BACKEND_URL] = session.backendUrl
    }
  }

  suspend fun saveBackendUrl(url: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.BACKEND_URL] = url
    }
  }

  suspend fun setDarkMode(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DARK_MODE] = enabled
    }
  }

  suspend fun updateLastSyncedAt(timestamp: Long) {
    context.dataStore.edit { prefs ->
      prefs[Keys.LAST_SYNCED_AT] = timestamp
    }
  }

  suspend fun clearSession() {
    context.dataStore.edit { prefs ->
      prefs.remove(Keys.SESSION_JSON)
    }
  }
}
