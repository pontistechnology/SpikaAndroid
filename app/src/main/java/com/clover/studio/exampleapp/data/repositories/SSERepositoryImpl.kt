package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.daos.MessageRecordsDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.MessageRecords
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.models.junction.RoomUser
import com.clover.studio.exampleapp.data.services.SSEService
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class SSERepositoryImpl @Inject constructor(
    private val sharedPrefs: SharedPreferencesRepository,
    private val sseService: SSEService,
    private val messageDao: MessageDao,
    private val messageRecordsDao: MessageRecordsDao,
    private val chatRoomDao: ChatRoomDao,
    private val appDatabase: AppDatabase,
    private val userDao: UserDao
) : SSERepository {
    override suspend fun syncMessageRecords() {
        var messageRecordsTimestamp = System.currentTimeMillis()
        if (messageRecordsDao.getMessageRecordsLocally().isNotEmpty()) {
            messageRecordsTimestamp =
                messageRecordsDao.getMessageRecordsLocally().last().createdAt
        }

        val response =
            sseService.syncMessageRecords(
                Tools.getHeaderMap(sharedPrefs.readToken()),
                messageRecordsTimestamp
            )

        val messageRecords: MutableList<MessageRecords> = ArrayList()
        for (record in response.data.messageRecords) {
            messageRecords.add(record)
        }
        messageRecordsDao.insert(messageRecords)
    }

    override suspend fun syncMessages() {
        val messageIds = ArrayList<Int>()
        val response =
            sseService.syncMessages(
                Tools.getHeaderMap(sharedPrefs.readToken())
            )

        val messages: MutableList<Message> = ArrayList()
        if (response.data?.list?.isNotEmpty() == true) {
            for (message in response.data.list) {
                messages.add(message)
                messageIds.add(message.id)
            }
            messageDao.insert(messages)

            sseService.sendMessageDelivered(
                Tools.getHeaderMap(sharedPrefs.readToken()),
                getMessageIdJson(messageIds)
            )
        }
    }

    override suspend fun syncUsers() {
        var userTimestamp = System.currentTimeMillis()

        if (userDao.getUsersLocally().isNotEmpty()) {
            userTimestamp = userDao.getUsersLocally().last().createdAt?.toLong()!!
        }

        val response =
            sseService.syncUsers(
                Tools.getHeaderMap(sharedPrefs.readToken()),
                userTimestamp
            )

        val users: MutableList<User> = ArrayList()
        if (response.data?.users?.isNotEmpty() == true) {
            for (user in response.data.users) {
                users.add(user)
            }
            userDao.insert(users)
        }
    }

    override suspend fun syncRooms() {
        var roomTimestamp = System.currentTimeMillis()

        if (chatRoomDao.getRoomsLocally().isNotEmpty()) {
            roomTimestamp = chatRoomDao.getRoomsLocally().last().createdAt!!
        }

        val response = sseService.syncRooms(
            Tools.getHeaderMap(sharedPrefs.readToken()),
            roomTimestamp
        )

        appDatabase.runInTransaction {
            CoroutineScope(Dispatchers.IO).launch {
                if (response.data?.rooms?.isNotEmpty() == true) {
                    val messagesList: MutableList<Message> = ArrayList()
                    for (room in response.data.rooms) {
                        val oldData = chatRoomDao.getRoomById(room.roomId)
                        chatRoomDao.updateRoomTable(oldData, room)

                        val messages = sseService.getMessagesForRooms(
                            Tools.getHeaderMap(sharedPrefs.readToken()),
                            room.roomId.toString()
                        )

                        if (messages.data?.list?.isNotEmpty() == true) {
                            for (message in messages.data.list) {
                                messagesList.add(message)
                            }
                        }
                    }
                    messageDao.insert(messagesList)
                }
            }
        }
    }

    override suspend fun sendMessageDelivered(messageId: Int) {
        sseService.sendMessageDelivered(
            Tools.getHeaderMap(sharedPrefs.readToken()),
            getMessageIdJson(arrayListOf(messageId))
        )
    }

    override suspend fun writeMessages(message: Message) {
        messageDao.insert(message)
    }

    override suspend fun writeMessageRecord(messageRecords: MessageRecords) {
        messageRecordsDao.insert(messageRecords)
    }

    override suspend fun writeUser(user: User) {
        userDao.insert(user)
    }

    override suspend fun writeRoom(room: ChatRoom) {
        val oldRoom = chatRoomDao.getRoomById(room.roomId)
        chatRoomDao.updateRoomTable(oldRoom, room)

        appDatabase.runInTransaction {
            CoroutineScope(Dispatchers.IO).launch {
                val users: MutableList<User> = ArrayList()
                val roomUsers: MutableList<RoomUser> = ArrayList()
                for (user in room.users) {
                    user.user?.let { users.add(it) }
                    roomUsers.add(
                        RoomUser(
                            room.roomId,
                            user.userId,
                            user.isAdmin
                        )
                    )
                }
                userDao.insert(users)
                chatRoomDao.insertRoomWithUsers(roomUsers)
            }
        }
    }

    override suspend fun deleteMessageRecord(messageRecords: MessageRecords) {
        messageRecordsDao.deleteMessageRecord(messageRecords)
    }

    override suspend fun deleteRoom(room: ChatRoom) {
        chatRoomDao.deleteRoom(room)
    }
}

interface SSERepository {
    suspend fun syncMessageRecords()
    suspend fun syncMessages()
    suspend fun syncUsers()
    suspend fun syncRooms()
    suspend fun sendMessageDelivered(messageId: Int)
    suspend fun writeMessages(message: Message)
    suspend fun writeMessageRecord(messageRecords: MessageRecords)
    suspend fun writeUser(user: User)
    suspend fun writeRoom(room: ChatRoom)
    suspend fun deleteMessageRecord(messageRecords: MessageRecords)
    suspend fun deleteRoom(room: ChatRoom)
}

private fun getMessageIdJson(messageIds: List<Int?>): JsonObject {
    val messageObject = JsonObject()
    val messageArray = JsonArray()

    for (id in messageIds) {
        messageArray.add(id)
    }
    messageObject.add(Const.JsonFields.MESSAGE_IDS, messageArray)
    return messageObject
}