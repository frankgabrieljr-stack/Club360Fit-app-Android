package com.club360fit.app.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.club360fit.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.auth
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class DeviceTokenRegistrationPayload(
    val platform: String,
    val token: String,
    val environment: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("device_id") val deviceId: String?
)

object PushRegistrationRepository {
    private const val TAG = "Club360Push"
    private val client = SupabaseClient.client

    fun syncAndroidFcmTokenIfPossible(context: Context) {
        debugLog("Starting Android FCM token sync")
        if (BuildConfig.FIREBASE_PROJECT_ID.isBlank()) {
            debugLog("Skipping FCM sync: FIREBASE_PROJECT_ID is blank")
            return
        }
        if (FirebaseApp.getApps(context).isEmpty()) {
            debugLog("Skipping FCM sync: FirebaseApp is not initialized")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isBlank()) {
                    debugLog("Firebase returned a blank FCM token")
                    return@addOnSuccessListener
                }
                debugLog("Firebase returned FCM token, registering with Supabase")
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    registerAndroidToken(
                        token = token,
                        deviceId = Settings.Secure.getString(
                            appContext.contentResolver,
                            Settings.Secure.ANDROID_ID
                        )
                    )
                }
            }
            .addOnFailureListener { error ->
                debugLog("Firebase failed to return an FCM token", error)
            }
    }

    suspend fun registerAndroidToken(token: String, deviceId: String?) = withContext(Dispatchers.IO) {
        val user = client.auth.currentUserOrNull()
        if (user == null) {
            debugLog("Skipping FCM token registration: no signed-in Supabase user")
            return@withContext
        }
        debugLog("Calling register-device-token for user ${user.id}")
        runCatching {
            val response = client.functions.invoke(
                function = "register-device-token",
                body = DeviceTokenRegistrationPayload(
                    platform = "android_fcm",
                    token = token,
                    environment = "production",
                    appVersion = BuildConfig.VERSION_NAME,
                    deviceId = deviceId
                ),
                headers = Headers.build {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            )
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "register-device-token response: ${response.bodyAsText()}")
            }
        }.onFailure { error ->
            debugLog("register-device-token failed", error)
        }
    }

    private fun debugLog(message: String, error: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (error == null) {
            Log.i(TAG, message)
        } else {
            Log.e(TAG, message, error)
        }
    }
}
