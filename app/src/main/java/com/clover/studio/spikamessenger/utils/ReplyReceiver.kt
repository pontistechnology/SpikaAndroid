package com.clover.studio.spikamessenger.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.clover.studio.spikamessenger.data.models.JsonMessage
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.data.repositories.ChatRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.utils.helpers.FilesHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatRepo: ChatRepositoryImpl

    @Inject
    lateinit var sharedPrefs: SharedPreferencesRepository

    private val replyMessage: MutableList<Message> = mutableListOf()

    override fun onReceive(context: Context?, intent: Intent) {
        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence("key_text_reply")
        val data = intent.getParcelableExtra("data") as RoomWithUsers?

        if (text != null && data != null) {
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(data.room.roomId.hashCode())
            Timber.d("Notification map: ${data.room.roomId.hashCode()}")

            CoroutineScope(Dispatchers.IO).launch {
                createTempTextMessage(text.toString(), data)
                sendMessage(data, text.toString())
            }
        }
    }

    private suspend fun sendMessage(
        data: RoomWithUsers?,
        text: String,
    ) {
        val jsonMessage = JsonMessage(
            msgText = text,
            mimeType = Const.JsonFields.TEXT_TYPE,
            fileId = 0,
            thumbId = 0,
            roomId = data?.room?.roomId,
            localId = replyMessage.first().localId,
            replyId = 0L
        )

        val jsonObject = jsonMessage.messageToJson()

        chatRepo.sendMessageNotificationReply(
            jsonObject = jsonObject,
            localId = replyMessage.first().localId.toString()
        )
        replyMessage.clear()
    }

    private suspend fun createTempTextMessage(
        text: String,
        data: RoomWithUsers,
    ) {
        val messageBody =
            MessageBody(
                referenceMessage = null,
                text = text,
                fileId = 1,
                thumbId = 1,
                file = null,
                thumb = null,
                subjectId = null,
                objectIds = null,
                type = "",
                objects = null,
                subject = ""
            )

        val tempMessage = Tools.createTemporaryMessage(
            id = FilesHelper.getUniqueRandomId(replyMessage),
            localUserId = sharedPrefs.readUserId(),
            roomId = data.room.roomId,
            messageType = Const.JsonFields.TEXT_TYPE,
            messageBody = messageBody
        )

        replyMessage.add(tempMessage)

        Timber.d("temp message:::::::::::::::: ${tempMessage}")

        chatRepo.storeMessageLocally(tempMessage)
    }
}