package com.clover.studio.spikamessenger.utils

import android.app.Notification
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Intent
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.networking.responses.FirebaseResponse
import com.clover.studio.spikamessenger.data.repositories.ChatRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepositoryImpl
import com.clover.studio.spikamessenger.ui.main.MainActivity
import com.clover.studio.spikamessenger.ui.main.chat.ChatScreenActivity
import com.clover.studio.spikamessenger.utils.helpers.GsonProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

const val CHANNEL_ID: String = "Spika App ID"
const val MAX_MESSAGES = 3
private val notificationMap = mutableMapOf<Int, Notification>()

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
            val gson = GsonProvider.gson
            val response =
                gson.fromJson(
                    jsonObject.toString(),
                    FirebaseResponse::class.java
                )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    chatRepo.storeMessageLocally(response.message)

                    val messageObject = JsonObject()
                    val messageArray = JsonArray()
                    messageArray.add(response.message.id)
                    messageObject.add(Const.JsonFields.MESSAGE_IDS, messageArray)

                    // TODO check why code below is not working. Might have been blocking push notifications
                    // TODO and crashing the app when receiving notification in background
//                chatRepo.sendMessageDelivered(messageObject)
                    chatRepo.getUnreadCount()

                    // Filter message if its from my user, don't show notification for it
                    if (sharedPrefs.readUserId() != null && sharedPrefs.readUserId() != response.message.fromUserId && !response.roomAttributes.muted && !MainApplication.isInForeground) {
                        val title: String
                        val content: String
                        if (response.messageAttributes.groupName.isNullOrEmpty()) {
                            content =
                                if (Const.JsonFields.TEXT_TYPE != response.message.type && Const.JsonFields.SYSTEM_TYPE != response.message.type) {
                                    getString(
                                        R.string.generic_shared,
                                        response.message.type.toString()
                                            .replaceFirstChar { it.uppercase() })
                                } else {
                                    response.message.body?.text.toString()
                                }
                            title = response.messageAttributes.fromUserName
                        } else {
                            content = if (Const.JsonFields.SYSTEM_TYPE == response.message.type) {
                                response.message.body?.text.toString()
                            } else if (response.message.type != Const.JsonFields.TEXT_TYPE) {
                                response.messageAttributes.fromUserName + ": " + getString(
                                    R.string.generic_shared,
                                    response.message.type.toString()
                                        .replaceFirstChar { it.uppercase() })
                            } else {
                                response.messageAttributes.fromUserName + ": " + response.message.body?.text.toString()
                            }
                            title = response.messageAttributes.groupName.toString()
                        }

                        Timber.d("Extras: ${response.message.roomId}")

                        val data = response.message.roomId?.let { chatRepo.getRoomUsers(it) }
                        val intent = Intent(baseContext, MainActivity::class.java)

                        val chatActivityIntent = Intent(baseContext, ChatScreenActivity::class.java)
                        chatActivityIntent.putExtra(
                            Const.IntentExtras.ROOM_ID_EXTRA,
                            data
                        )

                        val stackBuilder = TaskStackBuilder.create(baseContext).apply {
                            addParentStack(MainActivity::class.java)
                            addNextIntent(intent)
                            addNextIntent(chatActivityIntent)
                        }

                        val pendingIntent =
                            stackBuilder.getPendingIntent(
                                response.message.roomId!!,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                            )

                        val builder = NotificationCompat.Builder(baseContext, CHANNEL_ID)
                            .setSmallIcon(R.drawable.spika_base_logo)
                            .setContentTitle(title)
                            .setContentText(content)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(pendingIntent)
                            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                            .setAutoCancel(true)

                        // Check if there's an existing notification for this conversation.
                        if (notificationMap.containsKey(response.message.roomId)) {
                            // If there is, update it with the new message content.
                            val existingNotification = notificationMap[response.message.roomId]!!
                            val existingMessageCount =
                                existingNotification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)!!.size

                            // If the existing notification is already showing the maximum number of messages,
                            // remove the oldest message and update the message count.
                            if (existingMessageCount >= MAX_MESSAGES) {
                                val existingLines =
                                    existingNotification.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)!!
                                val newLines = Array(existingLines.size) { i ->
                                    if (i == 0) content else existingLines[i - 1]
                                }
                                val inboxStyle = NotificationCompat.InboxStyle()
                                for (i in 0 until MAX_MESSAGES) {
                                    inboxStyle.addLine(newLines[i])
                                }
                                inboxStyle.setBigContentTitle(
                                    existingNotification.extras.getCharSequence(
                                        Notification.EXTRA_TITLE
                                    )
                                )
                                // This can be used to add a summary to the notification which will
                                // tell the user how many more messages are there.
                                if (response.roomAttributes.unreadCount > 3) {
                                    inboxStyle.setSummaryText("+${response.roomAttributes.unreadCount - MAX_MESSAGES} more messages")
                                }
                                builder.setStyle(inboxStyle)
                                builder.setNumber(existingMessageCount + 1)
                            } else {
                                // Otherwise, just add the new message content and update the message count.
                                val inboxStyle = NotificationCompat.InboxStyle()
                                for (chars in existingNotification.extras.getCharSequenceArray(
                                    Notification.EXTRA_TEXT_LINES
                                )!!) {
                                    inboxStyle.addLine(chars)
                                }
                                inboxStyle.addLine(content)
                                inboxStyle.setBigContentTitle(
                                    notificationMap[response.message.roomId]?.extras?.getCharSequence(
                                        Notification.EXTRA_TITLE
                                    )
                                )
                                builder.setStyle(inboxStyle)
                                builder.setNumber(existingMessageCount + 1)
                            }

                            // Update the notification in the map.
                            notificationMap[response.message.roomId] = builder.build()
                        } else {
                            // If there's no existing notification for this conversation, create a new one.
                            val inboxStyle = NotificationCompat.InboxStyle()
                            inboxStyle.addLine(content)
                            inboxStyle.setBigContentTitle(title)
                            builder.setStyle(inboxStyle)
                            builder.setNumber(1)
                            notificationMap[response.message.roomId] = builder.build()
                        }

                        with(NotificationManagerCompat.from(baseContext)) {
                            // notificationId is a unique int for each notification that you must define
                            notify(
                                response.message.roomId.hashCode(),
                                notificationMap[response.message.roomId]!!
                            )
                        }
                    }
                } catch (ex: Exception) {
                    Tools.checkError(ex)
                }
            }
        } catch (ex: Exception) {
            Tools.checkError(ex)
        }
    }

}
