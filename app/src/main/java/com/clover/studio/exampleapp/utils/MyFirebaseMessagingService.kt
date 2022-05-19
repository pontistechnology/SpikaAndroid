package com.clover.studio.exampleapp.utils

import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.networking.FirebaseResponse
import com.clover.studio.exampleapp.data.repositories.ChatRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepositoryImpl
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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
    lateinit var chatRepo: ChatRepositoryImpl

    @Inject
    lateinit var sharedPrefs: SharedPreferencesRepository

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
                chatRepo.storeMessageLocally(response.message)

                val messageObject = JsonObject()
                val messageArray = JsonArray()
                messageArray.add(response.message.id)
                messageObject.add(Const.JsonFields.MESSAGE_IDS, messageArray)
                chatRepo.sendMessageDelivered(messageObject)

                // Filter message if its from my user, don't show notification for it
                if (sharedPrefs.readUserId() != null && sharedPrefs.readUserId() != response.message.fromUserId) {
                    val builder = NotificationCompat.Builder(baseContext, CHANNEL_ID)
                        .setSmallIcon(R.drawable.img_spika_logo)
                        .setContentTitle(getString(R.string.new_message))
                        .setContentText(response.message.body?.text)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                    with(NotificationManagerCompat.from(baseContext)) {
                        // notificationId is a unique int for each notification that you must define
                        notify(1511, builder.build())
                    }
                }
            }
        } catch (ex: Exception) {
            Tools.checkError(ex)
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}