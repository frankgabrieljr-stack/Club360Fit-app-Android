package com.club360fit.app

import android.provider.Settings
import android.util.Log
import com.club360fit.app.data.PushRegistrationRepository
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Club360MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (BuildConfig.DEBUG) {
            Log.i("Club360Push", "Firebase delivered refreshed FCM token")
        }
        CoroutineScope(Dispatchers.IO).launch {
            PushRegistrationRepository.registerAndroidToken(
                token = token,
                deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            )
        }
    }
}
