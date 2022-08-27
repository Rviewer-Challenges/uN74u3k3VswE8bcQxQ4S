package com.example.firebasechat.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.firebasechat.di.Modules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    System, Light, Dark;

    companion object{
        val Default = System
    }
}

interface SettingsStore {
    val defaultSettings: UserSettings
    val settings: StateFlow<UserSettings>

    suspend fun toggleThemeMode()
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.Default
)

@Singleton
class SettingsStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @Modules.DispatcherIO private val dispatcherIO: CoroutineDispatcher,
    private val externalScope: CoroutineScope,
) : SettingsStore {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("THEME_MODE_KEY")
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("USER_SETTINGS")

    override val defaultSettings = UserSettings()
    override val settings: StateFlow<UserSettings> = context.dataStore.data.map { store ->
        UserSettings(themeMode = ThemeMode.valueOf(store[THEME_MODE_KEY] ?: defaultSettings.themeMode.name))
    }.stateIn(externalScope, SharingStarted.Eagerly, defaultSettings)

    override suspend fun toggleThemeMode() {
        withContext(dispatcherIO) {
            context.dataStore.edit { store ->
                store[THEME_MODE_KEY] = when (settings.value.themeMode) {
                    ThemeMode.System -> ThemeMode.Dark
                    ThemeMode.Dark -> ThemeMode.Light
                    ThemeMode.Light -> ThemeMode.System
                }.name
            }
        }
    }
}