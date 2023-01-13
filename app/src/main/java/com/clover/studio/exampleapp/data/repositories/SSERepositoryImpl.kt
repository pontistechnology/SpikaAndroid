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
        // Write first time, next time update reaction
        // For now leave seen and delivered
        appDatabase.runInTransaction {
            CoroutineScope(Dispatchers.IO).launch {
                // Check if we have specific record in database for specific user
                // Add user id check
                // If not - add
                if (messageRecordsDao.getMessageId(messageRecords.messageId, messageRecords.userId) == null) {
                    Timber.d("insert")
                    messageRecordsDao.insert(messageRecords)
                } else {
                    // Else update seen or delivered type
                    if (messageRecords.type == "seen" || messageRecords.type == "delivered"){
                        Timber.d("Update")
                        Timber.d("type:::: ${messageRecords.type}")
                        messageRecordsDao.updateMessageRecords(
                            messageRecords.messageId,
                            messageRecords.type,
                            messageRecords.createdAt,
                            messageRecords.modifiedAt,
                            messageRecords.userId,
                        )
                    }
                    // If new record is message reaction update only reaction field leave type as seen or delivered
                    else {
                        messageRecordsDao.updateReaction(
                            messageRecords.messageId,
                            messageRecords.reaction!!,
                            messageRecords.userId,
                        )
                    }
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