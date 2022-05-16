package com.clover.studio.exampleapp.utils

import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.models.networking.FirebaseResponse
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepositoryImpl
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

val CHANNEL_ID: String = "Spika App ID"

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var messageDao: MessageDao

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Get updated InstanceID token.
        sendRegistrationToServer(token)
        Timber.d("FCM_refreshed_token: $token")
    }

    private fun sendRegistrationToServer(token: String) {
        if (!TextUtils.isEmpty(token)) {
            CoroutineScope(Dispatchers.IO).launch {
                val sharedPrefsRepo = SharedPreferencesRepositoryImpl(baseContext)
                sharedPrefsRepo.writePushToken(token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val notificationData = remoteMessage.data
        val notificationTitle = remoteMessage.notification?.title
        val notificationDescription = remoteMessage.notification?.body
        Timber.d("Notification title and body: $notificationData $notificationTitle $notificationDescription")

        // Write message to local db
        try {
            val jsonObject = JSONObject("$notificationData")
            val gson = Gson()
            val response =
                gson.fromJson(
                    jsonObject.toString(),
                    FirebaseResponse::class.java
                )
            CoroutineScope(Dispatchers.IO).launch {
                messageDao.insert(response.message)
            }
        } catch (ex: Exception) {
            Tools.checkError(ex)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.img_spika_logo)
            .setContentTitle(notificationTitle)
            .setContentText(notificationDescription)
            .setPriority(NotificationCompat.PRIORITY_MAX)
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(1511, builder.build())
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}