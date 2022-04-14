package com.clover.studio.exampleapp.utils

import android.text.TextUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Get updated InstanceID token.
        sendRegistrationToServer(token)
        Timber.d("FCM_refreshed_token: $token")
    }

    private fun sendRegistrationToServer(token: String) {
        if (!TextUtils.isEmpty(token)) {
            // TODO send token to server if needed
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val notificationData = remoteMessage.data
        Timber.d("$notificationData")
        for ((_, value) in notificationData) {
            Timber.d("FCM_message_body: $value")
            if (!TextUtils.isEmpty(value)) {
                // TODO set up notifications
            }
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}