package com.example.firebasechat.auth

import android.content.Context
import android.content.IntentSender
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.firebasechat.BuildConfig
import com.example.firebasechat.R
import com.example.firebasechat.auth.model.UserSnapshot
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import logcat.asLog
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object Unknown : AuthState()
    object SignedOut : AuthState()
    object SigningIn : AuthState()
    object SigningOut : AuthState()
    data class SignedIn(val user: FirebaseUser) : AuthState()
}

interface AuthManager {
    val authState: StateFlow<AuthState>

    fun onActivityCreate(newActivity: ComponentActivity)
    suspend fun signIn()
    fun signOut()
}

@Singleton
class AuthManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AuthManager, LifecycleEventObserver, FirebaseAuth.AuthStateListener {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState

    private val firebaseUsers = Firebase.database(BuildConfig.FIREBASE_URL).reference.child("users")

    private lateinit var activity: ComponentActivity
    private lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var oneTapClient: SignInClient

    init {
        Firebase.auth.addAuthStateListener(this)
    }

    override fun onActivityCreate(newActivity: ComponentActivity) {
        activity = newActivity
        // Register callback for lifecycle events
        activity.lifecycle.addObserver(this)
        // Register callback for sign in events
        val resultContract = ActivityResultContracts.StartIntentSenderForResult()
        launcher = newActivity.registerForActivityResult(resultContract) { result ->
            processSignInResult(result)
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> onAuthStateChanged(Firebase.auth)
            Lifecycle.Event.ON_DESTROY -> {
                // Remove observers
                activity.lifecycle.removeObserver(this)
                launcher.unregister()
            }
            else -> Unit
        }
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        _authState.value = when (val user = auth.currentUser) {
            null -> AuthState.SignedOut
            else -> AuthState.SignedIn(user)
        }
    }

    override suspend fun signIn() {
        _authState.value = AuthState.SigningIn
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .setFilterByAuthorizedAccounts(false) // True to only show accounts previously used to sign in.
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()

        try {
            oneTapClient = Identity.getSignInClient(context)
            val result = oneTapClient.beginSignIn(signInRequest).await()
            launcher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
        } catch (e: Exception) {
            if (e is IntentSender.SendIntentException) {
                logcat { "Couldn't start One Tap UI: ${e.localizedMessage}, failed to authorize" }
                _authState.value = AuthState.SignedOut
            } else {
                logcat { "No available credentials found: ${e.localizedMessage}, failed to authorie" }
                _authState.value = AuthState.SignedOut
            }
        }
    }

    private fun processSignInResult(result: ActivityResult) {
        try {
            val idToken = oneTapClient.getSignInCredentialFromIntent(result.data).googleIdToken
            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                Firebase.auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        // If it succeeds we'll get notified by [onAuthStateChanged]
                        if (!task.isSuccessful) {
                            logcat { "signInWithCredential:failure (${task.exception?.asLog()}), failed to authorize" }
                            _authState.value = AuthState.SignedOut
                        } else {
                            task.result.user?.let { user ->
                                // Store (or update) the user information
                                firebaseUsers.child(user.uid).setValue(UserSnapshot(user.displayName, user.photoUrl.toString()))
                            }
                        }
                    }
            } else {
                logcat { "No ID token (shouldn't happen), failed to authorize" }
                _authState.value = AuthState.SignedOut
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                CommonStatusCodes.CANCELED -> logcat { "One-tap dialog was closed." }
                CommonStatusCodes.NETWORK_ERROR -> logcat { "One-tap encountered a network error." }
                else -> logcat { "Couldn't get credential from result. (${e.localizedMessage})" }
            }
            _authState.value = AuthState.SignedOut
        }
    }

    override fun signOut() {
        _authState.value = AuthState.SigningOut
        Firebase.auth.signOut()
    }
}