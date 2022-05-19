package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.daos.MessageRecordsDao
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.networking.MessageRecordsResponse
import com.clover.studio.exampleapp.data.models.networking.MessageResponse
import com.clover.studio.exampleapp.data.services.ChatService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.JsonObject
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatService: ChatService,
    private val messageDao: MessageDao,
    private val messageRecordsDao: MessageRecordsDao,
    private val sharedPrefsRepo: SharedPreferencesRepository
) : ChatRepository {
    override suspend fun sendMessage(jsonObject: JsonObject): Message =
        chatService.sendMessage(getHeaderMap(sharedPrefsRepo.readToken()), jsonObject)

    override suspend fun getMessages(roomId: String) {
        val response = chatService.getMessages(getHeaderMap(sharedPrefsRepo.readToken()), roomId)

        if (response.data?.list != null) {
            for (message in response.data.list) {
                messageDao.insert(message)

//                val recordResponse = chatService.getMessageRecords(
//                    getHeaderMap(sharedPrefsRepo.readToken()),
//                    message.id.toString()
//                )
//
//                if (recordResponse.data.messageRecords.isNotEmpty()) {
//                    for (record in recordResponse.data.messageRecords) {
//                        messageRecordsDao.insert(record)
//                    }
//                }
            }
        }
    }

    override suspend fun getMessagesLiveData(roomId: Int): LiveData<List<Message>> =
        messageDao.getMessages(roomId)

    override suspend fun getMessagesTimestamp(timestamp: Int): MessageResponse =
        chatService.getMessagesTimestamp(getHeaderMap(sharedPrefsRepo.readToken()), timestamp)

    override suspend fun sendMessageDelivered(jsonObject: JsonObject) =
        chatService.sendMessageDelivered(getHeaderMap(sharedPrefsRepo.readToken()), jsonObject)

    override suspend fun storeMessageLocally(message: Message) {
        messageDao.insert(message)
    }

    override suspend fun deleteLocalMessages(messages: List<Message>) {
        if (messages.isNotEmpty()) {
            for (message in messages) {
                messageDao.deleteMessage(message)
            }
        }
    }
}

interface ChatRepository {
    suspend fun sendMessage(jsonObject: JsonObject): Message
    suspend fun getMessages(roomId: String)
    suspend fun getMessagesLiveData(roomId: Int): LiveData<List<Message>>
    suspend fun getMessagesTimestamp(timestamp: Int): MessageResponse
    suspend fun sendMessageDelivered(jsonObject: JsonObject): MessageRecordsResponse
    suspend fun storeMessageLocally(message: Message)
    suspend fun deleteLocalMessages(messages: List<Message>)
}