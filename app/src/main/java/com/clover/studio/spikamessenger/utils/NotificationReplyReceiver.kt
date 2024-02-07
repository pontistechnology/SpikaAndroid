package com.clover.studio.spikamessenger.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.clover.studio.spikamessenger.data.models.JsonMessage
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.data.repositories.ChatRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.utils.helpers.MessageHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatRepo: ChatRepositoryImpl

    @Inject
    lateinit var sharedPrefs: SharedPreferencesRepository

    private val replyMessage: MutableList<Message> = mutableListOf()

    override fun onReceive(context: Context?, intent: Intent) {
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(Const.PrefsData.NOTIFICATION_REPLY_KEY)
        val data =
            intent.getParcelableExtra(Const.PrefsData.NOTIFICATION_REPLY_DATA) as RoomWithUsers?

        if (text != null && data != null) {
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
        val tempMessage = sharedPrefs.readUserId()?.let { userId ->
            MessageHelper.createTempTextMessage(
                text = text,
                roomId = data.room.roomId,
                unsentMessages = replyMessage,
                localUserId = userId
            )
        }

        tempMessage?.let {
            replyMessage.add(tempMessage)
            chatRepo.storeMessageLocally(tempMessage)
        }
    }
}