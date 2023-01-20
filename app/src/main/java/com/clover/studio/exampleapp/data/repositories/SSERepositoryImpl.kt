package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.daos.MessageRecordsDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.entity.ChatRoom
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.MessageRecords
import com.clover.studio.exampleapp.data.models.entity.User
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
import java.util.stream.Collectors
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
        val messageRecordsTimestamp: Long
        if (sharedPrefs.readMessageRecordTimestamp() != 0L) {
            messageRecordsTimestamp =
                sharedPrefs.readMessageRecordTimestamp()!!
        } else {
            // This is only for first launch
            messageRecordsTimestamp = System.currentTimeMillis()
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
            writeRecord(record)
        }

        if (messageRecords.isNotEmpty()) {
            val maxTimestamp = messageRecords.maxByOrNull { it.createdAt }?.createdAt
            Timber.d("MaxTimestamp message records timestamps: $maxTimestamp")
            if (maxTimestamp!! > messageRecordsTimestamp) {
                sharedPrefs.writeMessageRecordTimestamp(maxTimestamp)
            }
        }
    }

    override suspend fun syncMessages() {
        Timber.d("Syncing messages")
        val messageTimestamp: Long
        if (sharedPrefs.readMessageTimestamp() != 0L) {
            messageTimestamp =
                sharedPrefs.readMessageTimestamp()!!
        } else {
            // This is only for first launch
            messageTimestamp = System.currentTimeMillis()
            sharedPrefs.writeMessageTimestamp(System.currentTimeMillis())
        }

        val messageIds = ArrayList<Int>()
        val response =
            sseService.syncMessages(
                Tools.getHeaderMap(sharedPrefs.readToken()),
                messageTimestamp
            )

        val messages: MutableList<Message> = ArrayList()
        if (response.data?.messages?.isNotEmpty() == true) {
            for (message in response.data.messages) {
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
                if (maxTimestamp!! > messageTimestamp) {
                    sharedPrefs.writeMessageTimestamp(maxTimestamp)
                }
            }
        }
    }

    override suspend fun syncUsers() {
        Timber.d("Syncing users")
        val userTimestamp: Long

        if (sharedPrefs.readUserTimestamp() != 0L) {
            userTimestamp =
                sharedPrefs.readUserTimestamp()!!
        } else {
            // This is only for first launch
            userTimestamp = System.currentTimeMillis()
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
                if (maxTimestamp!! > userTimestamp) {
                    sharedPrefs.writeUserTimestamp(maxTimestamp)
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
                            if (!room.deleted) {
                                Timber.d("Adding room ${room.name}")
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
                                Timber.d("MaxTimestamp rooms: $maxTimestamp, old timestamp = $roomTimestamp")
                                if (maxTimestamp!! > roomTimestamp) {
                                    sharedPrefs.writeRoomTimestamp(maxTimestamp)
                                }
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

    override suspend fun writeMessageRecord(messageRecords: MessageRecords) {
        appDatabase.runInTransaction {
            CoroutineScope(Dispatchers.IO).launch {
                writeRecord(messageRecords)
            }
        }
    }

    /**
     * The method writes message records to the database.
     * If the received message record (from sync or new message record) does not exist in the database, it writes a new data, and if it exists, it makes an update call (update type seen or reaction).
     * In this way, for type delivered or seen, there will be only one record for each user in the database. Also, for each user there will be one record of his reaction to the message (most recent reaction).
     * @param messageRecords
     */
    private suspend fun writeRecord(messageRecords: MessageRecords) {
        if (messageRecordsDao.getMessageRecordId(
                messageRecords.messageId,
                messageRecords.userId
            ) == null
        ) {
            messageRecordsDao.insert(messageRecords)
        } else {
            if (Const.JsonFields.SEEN == messageRecords.type) {
                messageRecordsDao.updateMessageRecords(
                    messageRecords.messageId,
                    messageRecords.type,
                    messageRecords.createdAt,
                    messageRecords.modifiedAt,
                    messageRecords.userId,
                )
            }
            else if (Const.JsonFields.REACTION == messageRecords.type) {
                if (messageRecordsDao.getMessageReactionId(
                        messageRecords.messageId,
                        messageRecords.userId
                    ) == null
                ) {
                    messageRecordsDao.insert(messageRecords)
                } else {
                    messageRecordsDao.updateReaction(
                        messageRecords.messageId,
                        messageRecords.reaction!!,
                        messageRecords.userId,
                        messageRecords.createdAt,
                    )
                }
            }
        }
    }

    override suspend fun writeUser(user: User) {
        userDao.insert(user)
    }

    override suspend fun writeRoom(room: ChatRoom) {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.runInTransaction {
                CoroutineScope(Dispatchers.IO).launch {
                    val oldRoom = chatRoomDao.getRoomById(room.roomId)
                    chatRoomDao.updateRoomTable(oldRoom, room)

                    val users: MutableList<User> = ArrayList()
                    val roomUsers: MutableList<RoomUser> = ArrayList()
                    for (user in room.users) {
                        user.user?.let { users.add(it) }
                        val roomUser = RoomUser(
                            room.roomId,
                            user.userId,
                            user.isAdmin
                        )
                        roomUsers.add(roomUser)
                    }

                    /**
                     * Compare old user list for specific room with the new user list fetched from
                     * backend. If filtered list is not empty that means that we have a user in
                     * the database who was deleted. Remove specific user and update the table with
                     * fresh data.
                     */
                    val oldRoomUsers = chatRoomDao.getRoomAndUsers(room.roomId).users

                    val roomUserLookup = roomUsers.stream()
                        .map { user -> user.userId }
                        .collect(Collectors.toCollection { HashSet() })

                    val filteredList = oldRoomUsers.stream()
                        .filter { user -> !roomUserLookup.contains(user.id) }
                        .map { user -> user.id }
                        .collect(Collectors.toList())

                    // Handle database operations
                    userDao.insert(users)
                    if (filteredList.isNotEmpty()) {
                        chatRoomDao.deleteRoomUsers(filteredList)
                    }
                    chatRoomDao.insertRoomWithUsers(roomUsers)
                }
            }
        }
    }

    override suspend fun deleteMessageRecord(messageRecords: MessageRecords) {
        messageRecordsDao.deleteMessageRecord(messageRecords)
    }

    override suspend fun deleteRoom(roomId: Int) =
        chatRoomDao.deleteRoom(roomId)
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
    suspend fun deleteRoom(roomId: Int)
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