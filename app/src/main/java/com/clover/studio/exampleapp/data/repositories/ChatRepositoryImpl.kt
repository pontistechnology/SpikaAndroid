package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.MessageRecordsResponse
import com.clover.studio.exampleapp.data.models.networking.MessageResponse
import com.clover.studio.exampleapp.data.services.ChatService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.JsonObject
import timber.log.Timber
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatService: ChatService,
    private val roomDao: ChatRoomDao,
    private val messageDao: MessageDao,
    private val sharedPrefsRepo: SharedPreferencesRepository
) : ChatRepository {
    override suspend fun sendMessage(jsonObject: JsonObject) {
        val response =
            chatService.sendMessage(getHeaderMap(sharedPrefsRepo.readToken()), jsonObject)
        Timber.d("Response message $response")
        response.data?.message?.let { messageDao.insert(it) }
    }

    override suspend fun getMessages(roomId: String) {
        val response = chatService.getMessages(getHeaderMap(sharedPrefsRepo.readToken()), roomId)

        if (response.data?.list != null) {
            for (message in response.data.list) {
                messageDao.insert(message)
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

    override suspend fun sendMessagesSeen(roomId: Int) =
        chatService.sendMessagesSeen(getHeaderMap(sharedPrefsRepo.readToken()), roomId)

    override suspend fun updatedRoomVisitedTimestamp(chatRoom: ChatRoom) {
        val oldRoom = roomDao.getRoomById(chatRoom.roomId)
        roomDao.updateRoomTable(oldRoom, chatRoom)
    }

    override suspend fun getRoomWithUsers(roomId: Int): RoomWithUsers =
        roomDao.getRoomAndUsers(roomId)

    override suspend fun updateRoom(jsonObject: JsonObject, roomId: Int) {
        val response =
            chatService.updateRoom(getHeaderMap(sharedPrefsRepo.readToken()), jsonObject, roomId)

        val oldRoom = roomDao.getRoomById(roomId)
        response.data?.room?.let { roomDao.updateRoomTable(oldRoom, it) }
    }
}

interface ChatRepository {
    suspend fun sendMessage(jsonObject: JsonObject)
    suspend fun getMessages(roomId: String)
    suspend fun getMessagesLiveData(roomId: Int): LiveData<List<Message>>
    suspend fun getMessagesTimestamp(timestamp: Int): MessageResponse
    suspend fun sendMessageDelivered(jsonObject: JsonObject): MessageRecordsResponse
    suspend fun storeMessageLocally(message: Message)
    suspend fun deleteLocalMessages(messages: List<Message>)
    suspend fun sendMessagesSeen(roomId: Int)
    suspend fun updatedRoomVisitedTimestamp(chatRoom: ChatRoom)
    suspend fun getRoomWithUsers(roomId: Int): RoomWithUsers
    suspend fun updateRoom(jsonObject: JsonObject, roomId: Int)
}