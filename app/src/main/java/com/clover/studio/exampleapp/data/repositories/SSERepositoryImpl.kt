package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.daos.MessageRecordsDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.MessageRecords
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.services.SSEService
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SSERepositoryImpl @Inject constructor(
    private val sharedPrefs: SharedPreferencesRepository,
    private val sseService: SSEService,
    private val messageDao: MessageDao,
    private val messageRecordsDao: MessageRecordsDao,
    private val chatRoomDao: ChatRoomDao,
    private val userDao: UserDao
) : SSERepository {
    override suspend fun syncMessageRecords() {
        var messageRecordsTimestamp = 0L
        if (messageRecordsDao.getMessageRecordsLocally().isNotEmpty()) {
            messageRecordsTimestamp =
                messageRecordsDao.getMessageRecordsLocally().last().createdAt
        }

        val response =
            sseService.syncMessageRecords(
                Tools.getHeaderMap(sharedPrefs.readToken()),
                messageRecordsTimestamp
            )

        for (record in response.data.messageRecords)
            messageRecordsDao.insert(record)
    }

    override suspend fun syncMessages() {
        val messageIds = ArrayList<Int>()
        val response =
            sseService.syncMessages(
                Tools.getHeaderMap(sharedPrefs.readToken())
            )

        if (response.data?.list?.isNotEmpty() == true) {
            for (message in response.data.list) {
                messageDao.insert(message)
                messageIds.add(message.id)
            }

            sseService.sendMessageDelivered(
                Tools.getHeaderMap(sharedPrefs.readToken()),
                getMessageIdJson(messageIds)
            )
        }
    }

    override suspend fun syncUsers() {
        var userTimestamp = 0L

        if (userDao.getUsersLocally().isNotEmpty()) {
            userTimestamp = userDao.getUsersLocally().last().createdAt?.toLong()!!
        }

        val response =
            sseService.syncUsers(
                Tools.getHeaderMap(sharedPrefs.readToken()),
                userTimestamp
            )

        if (response.data?.users?.isNotEmpty() == true) {
            for (user in response.data.users) {
                userDao.insert(user)
            }
        }
    }

    override suspend fun syncRooms() {
        var roomTimestamp = 0L

        if (chatRoomDao.getRoomsLocally().isNotEmpty()) {
            roomTimestamp = chatRoomDao.getRoomsLocally().last().createdAt!!
        }

        val response = sseService.syncRooms(
            Tools.getHeaderMap(sharedPrefs.readToken()),
            roomTimestamp
        )

        if (response.data?.rooms?.isNotEmpty() == true) {
            for (room in response.data.rooms) {
                withContext(Dispatchers.IO) {
                    val oldData = chatRoomDao.getRoomById(room.roomId)
                    chatRoomDao.updateRoomTable(oldData, room)

                    val messages = sseService.getMessagesForRooms(
                        Tools.getHeaderMap(sharedPrefs.readToken()),
                        room.roomId.toString()
                    )

                    if (messages.data?.list?.isNotEmpty() == true) {
                        for (message in messages.data.list) {
                            messageDao.insert(message)
                        }
                    }
                }
            }
        }
    }

    override suspend fun sendMessageDelivered(messageId: Int) {
        sseService.sendMessageDelivered(
            Tools.getHeaderMap(sharedPrefs.readToken()),
            getMessageIdJson(ArrayList(messageId))
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