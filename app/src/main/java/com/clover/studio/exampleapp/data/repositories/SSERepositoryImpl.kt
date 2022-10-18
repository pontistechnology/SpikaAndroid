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
import com.clover.studio.exampleapp.data.models.networking.ChatRoomUpdate
import com.clover.studio.exampleapp.data.services.SSEService
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
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
        Timber.d("Syncing message records")
        var messageRecordsTimestamp = 0L
        if (sharedPrefs.readMessageRecordTimestamp() != 0L) {
            messageRecordsTimestamp =
                sharedPrefs.readMessageRecordTimestamp()!!
        } else {
            // This is only for first launch
            sharedPrefs.writeMessageRecordTimestamp(System.currentTimeMillis())
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

        if (messageRecords.isNotEmpty()) {
            val maxTimestamp = messageRecords.maxByOrNull { it.createdAt }?.createdAt
            Timber.d("MaxTimestamp message records timestamps: $maxTimestamp")
            sharedPrefs.writeMessageRecordTimestamp(messageRecords.maxByOrNull { it.createdAt }!!.createdAt)
        }
    }

    override suspend fun syncMessages() {
        Timber.d("Syncing messages")
        var messageTimestamp = 0L
        if (sharedPrefs.readMessageTimestamp() != 0L) {
            messageTimestamp =
                sharedPrefs.readMessageTimestamp()!!
        } else {
            // This is only for first launch
            sharedPrefs.writeMessageTimestamp(System.currentTimeMillis())
        }

        val messageIds = ArrayList<Int>()
        val response =
            sseService.syncMessages(
                Tools.getHeaderMap(sharedPrefs.readToken()),
                messageTimestamp
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

            if (messages.isNotEmpty()) {
                val maxTimestamp = messages.maxByOrNull { it.modifiedAt!! }?.modifiedAt
                Timber.d("MaxTimestamp messages: $maxTimestamp")
                messages.maxByOrNull { it.modifiedAt!! }?.modifiedAt?.let {
                    sharedPrefs.writeMessageTimestamp(
                        it
                    )
                }
            }
        }
    }

    override suspend fun syncUsers() {
        Timber.d("Syncing users")
        var userTimestamp = 0L

        if (sharedPrefs.readUserTimestamp() != 0L) {
            userTimestamp =
                sharedPrefs.readUserTimestamp()!!
        } else {
            // This is only for first launch
            sharedPrefs.writeUserTimestamp(System.currentTimeMillis())
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

            if (users.isNotEmpty()) {
                val maxTimestamp = users.maxByOrNull { it.modifiedAt!! }?.modifiedAt
                Timber.d("MaxTimestamp users: $maxTimestamp")
                users.maxByOrNull { it.modifiedAt!! }?.modifiedAt?.let {
                    sharedPrefs.writeUserTimestamp(
                        it
                    )
                }
            }
        }
    }

    override suspend fun syncRooms() {
        Timber.d("Syncing rooms")

        val roomTimestamp: Long = sharedPrefs.readRoomTimestamp()!!

        val response = sseService.syncRooms(
            Tools.getHeaderMap(sharedPrefs.readToken()),
            roomTimestamp
        )

        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.runInTransaction {
                CoroutineScope(Dispatchers.IO).launch {
                    if (response.data?.rooms != null) {
                        val users: MutableList<User> = ArrayList()
                        val rooms: MutableList<ChatRoom> = ArrayList()
                        val roomUsers: MutableList<RoomUser> = ArrayList()
                        val chatRooms: MutableList<ChatRoomUpdate> = ArrayList()
                        for (room in response.data.rooms) {
                            val oldData = chatRoomDao.getRoomById(room.roomId)
                            chatRooms.add(ChatRoomUpdate(oldData, room))
                            rooms.add(room)

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
                        }
                        chatRoomDao.updateRoomTable(chatRooms)
                        userDao.insert(users)
                        chatRoomDao.insertRoomWithUsers(roomUsers)

                        if (rooms.isNotEmpty()) {
                            val maxTimestamp = rooms.maxByOrNull { it.modifiedAt!! }?.modifiedAt
                            Timber.d("MaxTimestamp rooms: $maxTimestamp")
                            rooms.maxByOrNull { it.modifiedAt!! }?.modifiedAt?.let {
                                sharedPrefs.writeRoomTimestamp(
                                    it
                                )
                            }
                        }
                    }
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

    override suspend fun deleteMessage(message: Message) {
        messageDao.deleteMessage(message)
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
    suspend fun deleteMessage(message: Message)
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