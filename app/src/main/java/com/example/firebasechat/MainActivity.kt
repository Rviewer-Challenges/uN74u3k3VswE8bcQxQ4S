package com.example.firebasechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.firebasechat.auth.AuthManager
import com.example.firebasechat.settings.SettingsStore
import com.example.firebasechat.ui.App
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        authManager.onActivityCreate(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            App(settingsStore.settings.collectAsState(initial = settingsStore.defaultSettings))
        }
    }
}