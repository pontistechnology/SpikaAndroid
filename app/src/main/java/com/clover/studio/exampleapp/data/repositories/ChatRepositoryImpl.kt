package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.entity.ChatRoom
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.junction.RoomUser
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.Settings
import com.clover.studio.exampleapp.data.services.ChatService
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatService: ChatService,
    private val roomDao: ChatRoomDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val appDatabase: AppDatabase,
    private val sharedPrefsRepo: SharedPreferencesRepository
) : ChatRepository {
    override suspend fun sendMessage(jsonObject: JsonObject) {
        val response =
            chatService.sendMessage(getHeaderMap(sharedPrefsRepo.readToken()), jsonObject)
        Timber.d("Response message $response")
        response.data?.message?.let { messageDao.insert(it) }
    }

    override suspend fun storeMessageLocally(message: Message) {
        messageDao.insert(message)
    }

    override suspend fun deleteLocalMessages(messages: List<Message>) {
//        if (messages.isNotEmpty()) {
            val messagesIds = mutableListOf<Long>()
            for (message in messages) {
                messagesIds.add(message.id.toLong())
            }
            // TODO remove this later
        messagesIds.add(0)
        messagesIds.add(1)
        messagesIds.add(2)
            messageDao.deleteMessage(messagesIds)
//        }
    }

    override suspend fun deleteLocalMessage(message: Message) {
        messageDao.deleteMessage(message)
    }

    override suspend fun sendMessagesSeen(roomId: Int) =
        chatService.sendMessagesSeen(getHeaderMap(sharedPrefsRepo.readToken()), roomId)

    override suspend fun updatedRoomVisitedTimestamp(chatRoom: ChatRoom) {
        val oldRoom = roomDao.getRoomById(chatRoom.roomId)
        roomDao.updateRoomTable(oldRoom, chatRoom)
    }

    override suspend fun getRoomWithUsersLiveData(roomId: Int): LiveData<RoomWithUsers> =
        roomDao.getRoomAndUsersLiveData(roomId)

    override suspend fun getRoomWithUsers(roomId: Int) =
        roomDao.getRoomAndUsers(roomId)

    override suspend fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) {
        val response =
            chatService.updateRoom(getHeaderMap(sharedPrefsRepo.readToken()), jsonObject, roomId)

        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.runInTransaction {
                CoroutineScope(Dispatchers.IO).launch {
                    val oldRoom = roomDao.getRoomById(roomId)
                    response.data?.room?.let { roomDao.updateRoomTable(oldRoom, it) }

                    val users: MutableList<User> = ArrayList()
                    val roomUsers: MutableList<RoomUser> = ArrayList()
                    if (response.data?.room != null) {
                        val room = response.data.room

                        // Delete Room User if id has been passed through
                        if (userId != 0) {
                            roomDao.deleteRoomUser(RoomUser(roomId, userId, false))
                        }

                        for (user in room.users) {
                            user.user?.let { users.add(it) }
                            roomUsers.add(
                                RoomUser(
                                    room.roomId, user.userId, user.isAdmin
                                )
                            )
                        }
                        userDao.insert(users)
                        roomDao.insertRoomWithUsers(roomUsers)
                    }
                }
            }
        }
    }

    override suspend fun getRoomUserById(roomId: Int, userId: Int): Boolean? =
        roomDao.getRoomUserById(roomId, userId).isAdmin

    override suspend fun getChatRoomAndMessageAndRecordsById(roomId: Int): LiveData<RoomAndMessageAndRecords> =
        roomDao.getChatRoomAndMessageAndRecordsById(roomId)

    override suspend fun muteRoom(roomId: Int) =
        chatService.muteRoom(getHeaderMap(sharedPrefsRepo.readToken()), roomId)

    override suspend fun unmuteRoom(roomId: Int) =
        chatService.unmuteRoom(getHeaderMap(sharedPrefsRepo.readToken()), roomId)

    override suspend fun getUserSettings(): List<Settings> =
        chatService.getSettings(getHeaderMap(sharedPrefsRepo.readToken())).data.settings


    override suspend fun getSingleRoomData(roomId: Int): RoomAndMessageAndRecords =
        roomDao.getSingleRoomData(roomId)

    override suspend fun sendReaction(jsonObject: JsonObject) =
        chatService.postReaction(getHeaderMap(sharedPrefsRepo.readToken()), jsonObject)

    /* TODO: Commented methods can later be used to delete reactions
    override suspend fun deleteReaction(recordId: Int, userId: Int) {
        chatService.deleteReaction(getHeaderMap(sharedPrefsRepo.readToken()), recordId)
        chatRoomDao.deleteReactionRecord(recordId, userId)
        Timber.d("id:::::: $recordId")
    }

    override suspend fun deleteAllReactions(messageId: Int) {
        chatRoomDao.deleteAllReactions(messageId)
    }*/

    override suspend fun deleteRoom(roomId: Int) {
        val response = chatService.deleteRoom(getHeaderMap(sharedPrefsRepo.readToken()), roomId)
        if (response.data?.room?.deleted == true) {
            roomDao.deleteRoom(roomId)
        }
    }

    override suspend fun deleteMessage(messageId: Int, target: String) {
        val response =
            chatService.deleteMessage(getHeaderMap(sharedPrefsRepo.readToken()), messageId, target)

        // Just replace old message with new one. Deleted message just has a body with new text
        if (response.data?.message != null) {
            val deletedMessage = response.data.message
            deletedMessage.type = Const.JsonFields.TEXT
            messageDao.insert(deletedMessage)
        }
    }

    override suspend fun editMessage(messageId: Int, jsonObject: JsonObject) {
        val response = chatService.editMessage(
            getHeaderMap(sharedPrefsRepo.readToken()),
            messageId,
            jsonObject
        )
        if (response.data?.message != null) {
            messageDao.insert(response.data.message)
        }
    }
}

interface ChatRepository {
    suspend fun sendMessage(jsonObject: JsonObject)
    suspend fun storeMessageLocally(message: Message)
    suspend fun deleteLocalMessages(messages: List<Message>)
    suspend fun deleteLocalMessage(message: Message)
    suspend fun sendMessagesSeen(roomId: Int)
    suspend fun updatedRoomVisitedTimestamp(chatRoom: ChatRoom)
    suspend fun getRoomWithUsersLiveData(roomId: Int): LiveData<RoomWithUsers>
    suspend fun getRoomWithUsers(roomId: Int): RoomWithUsers
    suspend fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int)
    suspend fun getRoomUserById(roomId: Int, userId: Int): Boolean?
    suspend fun muteRoom(roomId: Int)
    suspend fun unmuteRoom(roomId: Int)
    suspend fun getUserSettings(): List<Settings>
    suspend fun getSingleRoomData(roomId: Int): RoomAndMessageAndRecords
    suspend fun getChatRoomAndMessageAndRecordsById(roomId: Int): LiveData<RoomAndMessageAndRecords>
    suspend fun sendReaction(jsonObject: JsonObject)

    // suspend fun deleteReaction(recordId: Int, userId: Int)
    // suspend fun deleteAllReactions(messageId: Int)
    // suspend fun deleteReaction(id: Int)
    suspend fun deleteRoom(roomId: Int)
    suspend fun deleteMessage(messageId: Int, target: String)
    suspend fun editMessage(messageId: Int, jsonObject: JsonObject)
}