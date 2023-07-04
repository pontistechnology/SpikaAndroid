package com.clover.studio.spikamessenger.data.repositories

import com.clover.studio.spikamessenger.data.AppDatabase
import com.clover.studio.spikamessenger.data.daos.BaseDao
import com.clover.studio.spikamessenger.data.daos.ChatRoomDao
import com.clover.studio.spikamessenger.data.daos.MessageDao
import com.clover.studio.spikamessenger.data.daos.MessageRecordsDao
import com.clover.studio.spikamessenger.data.daos.RoomUserDao
import com.clover.studio.spikamessenger.data.daos.UserDao
import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.junction.RoomUser
import com.clover.studio.spikamessenger.data.repositories.data_sources.SSERemoteDataSource
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.clover.studio.spikamessenger.utils.helpers.RestOperations.performRestOperation
import com.clover.studio.spikamessenger.utils.helpers.RestOperations.queryDatabaseCoreData
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.stream.Collectors
import javax.inject.Inject


class SSERepositoryImpl @Inject constructor(
    private val sseRemoteDataSource: SSERemoteDataSource,
    private val sharedPrefs: SharedPreferencesRepository,
    private val messageDao: MessageDao,
    private val messageRecordsDao: MessageRecordsDao,
    private val chatRoomDao: ChatRoomDao,
    private val roomUserDao: RoomUserDao,
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

//        val response =
//            performRestOperation(
//                networkCall = { sseRemoteDataSource.syncMessageRecords(messageRecordsTimestamp) }
//            )
//
//        CoroutineScope(Dispatchers.IO).launch {
//            appDatabase.runInTransaction {
//                CoroutineScope(Dispatchers.IO).launch {
//                    val messageRecords: MutableList<MessageRecords> = ArrayList()
//                    val messageRecordsUpdates: MutableList<MessageRecords> = ArrayList()
//
//                    if (response.responseData?.data?.list != null) {
//                        for (record in response.responseData.data.list) {
//                            val databaseRecords = queryDatabaseCoreData(
//                                databaseQuery = {
//                                    messageRecordsDao.getMessageRecordId(
//                                        record.messageId,
//                                        record.userId,
//                                    )
//                                }
//                            ).responseData
//
//                            if (databaseRecords == null) {
//                                messageRecords.add(record)
//                            } else
//                                if (Const.JsonFields.SEEN == record.type) {
//                                    messageRecordsUpdates.add(record)
//                                } else if (Const.JsonFields.REACTION == record.type) {
//                                    val databaseReaction = queryDatabaseCoreData(
//                                        databaseQuery = {
//                                            messageRecordsDao.getMessageReactionId(
//                                                record.messageId,
//                                                record.userId
//                                            )
//                                        }
//                                    ).responseData
//                                    if (databaseReaction == null) {
//                                        messageRecords.add(record)
//                                    } else {
//                                        messageRecordsUpdates.add(record)
//                                    }
//                                }
//                        }
//                    }
//
//                    queryDatabaseCoreData(
//                        databaseQuery = { messageRecordsDao.upsert(messageRecords) }
//                    )
//
//                    // Since this is a transaction method this loop should insert all or none
//                    messageRecordsUpdates.forEach {
//                        queryDatabaseCoreData(
//                            databaseQuery = {
//                                messageRecordsDao.updateMessageRecords(
//                                    it.userId,
//                                    it.type,
//                                    it.createdAt,
//                                    it.modifiedAt,
//                                    it.userId
//                                )
//                            }
//                        )
//                    }
//
//                    if (messageRecords.isNotEmpty()) {
//                        val maxTimestamp = messageRecords.maxByOrNull { it.createdAt }?.createdAt
//                        Timber.d("MaxTimestamp message records timestamps: $maxTimestamp")
//                        if (maxTimestamp != null && maxTimestamp > messageRecordsTimestamp) {
//                            sharedPrefs.writeMessageRecordTimestamp(maxTimestamp)
//                        }
//                    }
//                }
//            }
//        }
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

//        val messageIds = ArrayList<Int>()
//        val response = performRestOperation(
//            networkCall = { sseRemoteDataSource.syncMessages(messageTimestamp) }
//        )
//
//        val messages: MutableList<Message> = ArrayList()
//        if (response.responseData?.data?.messages?.isNotEmpty() == true) {
//            for (message in response.responseData.data.messages) {
//                messages.add(message)
//                messageIds.add(message.id)
//            }
//
//            queryDatabaseCoreData(
//                databaseQuery = { messageDao.upsert(messages) }
//            )
//
//            performRestOperation(
//                networkCall = { sseRemoteDataSource.sendMessageDelivered(getMessageIdJson(messageIds)) }
//            )
//
//            if (messages.isNotEmpty()) {
//                val maxTimestamp = messages.maxByOrNull { it.modifiedAt!! }?.modifiedAt
//                Timber.d("MaxTimestamp messages: $maxTimestamp")
//                if (maxTimestamp != null && maxTimestamp > messageTimestamp) {
//                    sharedPrefs.writeMessageTimestamp(maxTimestamp)
//                }
//            }
//        }
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
            syncNextBatch(
                lastUpdate = userTimestamp,
                dao = userDao,
                networkCall = { sseRemoteDataSource.syncUsers(userTimestamp, it) },
                saveCallResult = {
                    it.data?.list?.let { users -> userDao.upsert(users) }
                },
                shouldSyncMore = {
                    it.data?.hasNext == true
                },
                page = 1
            )

        if (Resource.Status.SUCCESS == response.status) {
//            val maxTimestamp = users.maxByOrNull { it.modifiedAt!! }?.modifiedAt
//            Timber.d("MaxTimestamp users: $maxTimestamp")
//            if (maxTimestamp != null && maxTimestamp > userTimestamp) {
//                sharedPrefs.writeUserTimestamp(maxTimestamp)
//            }
        }

        val users: MutableList<User> = ArrayList()
        if (response.responseData?.data?.users?.isNotEmpty() == true) {
            for (user in response.responseData.data.users) {
                users.add(user)
            }

            queryDatabaseCoreData(
                databaseQuery = { userDao.upsert(users) }
            )

            if (users.isNotEmpty()) {
                val maxTimestamp = users.maxByOrNull { it.modifiedAt!! }?.modifiedAt
                Timber.d("MaxTimestamp users: $maxTimestamp")
                if (maxTimestamp != null && maxTimestamp > userTimestamp) {
                    sharedPrefs.writeUserTimestamp(maxTimestamp)
                }
            }
        }
    }

    override suspend fun syncRooms() {
        Timber.d("Syncing rooms")
        val roomTimestamp: Long = sharedPrefs.readRoomTimestamp()!!
//        val response = performRestOperation(
//            networkCall = { sseRemoteDataSource.syncRooms(roomTimestamp) }
//        )
//
//        CoroutineScope(Dispatchers.IO).launch {
//            appDatabase.runInTransaction {
//                CoroutineScope(Dispatchers.IO).launch {
//                    if (response.responseData?.data?.rooms != null) {
//                        val users: MutableList<User> = ArrayList()
//                        val rooms: MutableList<ChatRoom> = ArrayList()
//                        val roomUsers: MutableList<RoomUser> = ArrayList()
//                        for (room in response.responseData.data.rooms) {
//                            if (!room.deleted) {
//                                Timber.d("Adding room ${room.name}")
//
//                                for (user in room.users) {
//                                    user.user?.let { users.add(it) }
//                                    roomUsers.add(
//                                        RoomUser(
//                                            room.roomId,
//                                            user.userId,
//                                            user.isAdmin
//                                        )
//                                    )
//                                }
//                                rooms.add(room)
//                            }
//                        }
//                        queryDatabaseCoreData(
//                            databaseQuery = { chatRoomDao.upsert(rooms) }
//                        )
//                        queryDatabaseCoreData(
//                            databaseQuery = { userDao.upsert(users) }
//                        )
//                        queryDatabaseCoreData(
//                            databaseQuery = { roomUserDao.upsert(roomUsers) }
//                        )
//                        if (rooms.isNotEmpty()) {
//                            val maxTimestamp = rooms.maxByOrNull { it.modifiedAt!! }?.modifiedAt
//                            Timber.d("MaxTimestamp rooms: $maxTimestamp, old timestamp = $roomTimestamp")
//                            if (maxTimestamp != null && maxTimestamp > roomTimestamp) {
//                                sharedPrefs.writeRoomTimestamp(maxTimestamp)
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

    suspend fun syncContacts(shouldRefresh: Boolean = false) {
        syncContacts(userDao, shouldRefresh, sharedPrefs, sseRemoteDataSource)
    }

    override suspend fun sendMessageDelivered(messageId: Int) {
        performRestOperation(
            networkCall = {
                sseRemoteDataSource.sendMessageDelivered(
                    getMessageIdJson(
                        arrayListOf(
                            messageId
                        )
                    )
                )
            }
        )
    }

    override suspend fun writeMessages(message: Message) {
        CoroutineScope(Dispatchers.IO).launch {
            queryDatabaseCoreData(
                databaseQuery = { messageDao.upsert(message) }
            )
        }
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
        /** Update seenCount / deliveredCount from NEW_MESSAGE_RECORD event */
        if (messageRecords.recordMessage != null) {
            val databaseRecord = queryDatabaseCoreData {
                messageDao.getMessage(messageRecords.recordMessage.id)
            }.responseData

            if (databaseRecord != null) {
                if (databaseRecord.seenCount != null) {
                    if (messageRecords.recordMessage.seenCount > databaseRecord.seenCount) {
                        queryDatabaseCoreData {
                            messageDao.updateMessageSeenCount(
                                messageRecords.recordMessage.id,
                                messageRecords.recordMessage.seenCount,
                            )
                        }
                    }
                }
                if (databaseRecord.deliveredCount != null) {
                    if (messageRecords.recordMessage.deliveredCount > databaseRecord.deliveredCount) {
                        queryDatabaseCoreData {
                            messageDao.updateMessageDeliveredCount(
                                messageRecords.recordMessage.id,
                                messageRecords.recordMessage.deliveredCount,
                            )
                        }
                    }
                }
            }
        }
        /***/

        /** Update seen / reaction */
        val databaseRecords = queryDatabaseCoreData(
            databaseQuery = {
                messageRecordsDao.getMessageRecordId(
                    messageRecords.messageId,
                    messageRecords.userId
                )
            }
        ).responseData

        if (databaseRecords == null) {
            queryDatabaseCoreData(
                databaseQuery = { messageRecordsDao.upsert(messageRecords) }
            )
        } else {
            if (Const.JsonFields.SEEN == messageRecords.type) {
                queryDatabaseCoreData(
                    databaseQuery = {
                        messageRecordsDao.updateMessageRecords(
                            messageRecords.messageId,
                            messageRecords.type,
                            messageRecords.createdAt,
                            messageRecords.modifiedAt,
                            messageRecords.userId,
                        )
                    }
                )
            } else if (Const.JsonFields.REACTION == messageRecords.type) {
                val databaseReaction = queryDatabaseCoreData(
                    databaseQuery = {
                        messageRecordsDao.getMessageReactionId(
                            messageRecords.messageId,
                            messageRecords.userId
                        )
                    }
                ).responseData
                if (databaseReaction == null) {
                    queryDatabaseCoreData(
                        databaseQuery = { messageRecordsDao.upsert(messageRecords) }
                    )
                } else {
                    queryDatabaseCoreData(
                        databaseQuery = {
                            messageRecordsDao.updateReaction(
                                messageRecords.messageId,
                                messageRecords.reaction!!,
                                messageRecords.userId,
                                messageRecords.createdAt,
                            )
                        }
                    )
                }
            }
        }
    }

    override suspend fun writeUser(user: User) {
        queryDatabaseCoreData(
            databaseQuery = { userDao.upsert(user) }
        )
    }

    override suspend fun writeRoom(room: ChatRoom) {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.runInTransaction {
                CoroutineScope(Dispatchers.IO).launch {
                    queryDatabaseCoreData(
                        databaseQuery = { chatRoomDao.upsert(room) }
                    )

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

                    val oldRoomUsers = queryDatabaseCoreData(
                        databaseQuery = { chatRoomDao.getRoomAndUsers(room.roomId) }
                    ).responseData?.users

                    val roomUserLookup = roomUsers.stream()
                        .map { user -> user.userId }
                        .collect(Collectors.toCollection { HashSet() })

                    val filteredList = oldRoomUsers?.stream()
                        ?.filter { user -> !roomUserLookup.contains(user.id) }
                        ?.map { user -> user.id }
                        ?.collect(Collectors.toList())

                    // Handle database operations
                    queryDatabaseCoreData(
                        databaseQuery = { userDao.upsert(users) }
                    )
                    if (filteredList?.isNotEmpty() == true) {
                        queryDatabaseCoreData(
                            databaseQuery = { roomUserDao.deleteRoomUsers(filteredList) }
                        )
                    }
                    queryDatabaseCoreData(
                        databaseQuery = { roomUserDao.upsert(roomUsers) }
                    )
                }
            }
        }
    }

    override suspend fun deleteMessageRecord(messageRecords: MessageRecords) {
        queryDatabaseCoreData(
            databaseQuery = { messageRecordsDao.delete(messageRecords) }
        )
    }

    override suspend fun deleteRoom(roomId: Int) {
        queryDatabaseCoreData(
            databaseQuery = { chatRoomDao.updateRoomDeleted(roomId, true) }
        )
    }

    override suspend fun resetUnreadCount(roomId: Int) {
        queryDatabaseCoreData(
            databaseQuery = { chatRoomDao.resetUnreadCount(roomId) }
        )
    }

    override suspend fun getUnreadCount() {
        val response = performRestOperation(
            networkCall = { sseRemoteDataSource.getUnreadCount() }
        )

        CoroutineScope(Dispatchers.IO).launch {
            if (response.responseData?.data?.unreadCounts != null) {
                val currentRooms = chatRoomDao.getAllRooms()
                val roomsToUpdate: MutableList<ChatRoom> = ArrayList()
                for (room in currentRooms) {
                    room.unreadCount = 0
                    for (item in response.responseData.data.unreadCounts) {
                        if (item.roomId == room.roomId) {
                            room.unreadCount = item.unreadCount
                            break
                        }
                    }
                    if (!room.deleted) roomsToUpdate.add(room)
                }
                Timber.d("Rooms to update: $roomsToUpdate")
                queryDatabaseCoreData(
                    databaseQuery = { chatRoomDao.upsert(roomsToUpdate) }
                )
            }
        }
    }

    override suspend fun getAppMode(): Boolean {
        val response = performRestOperation(
            networkCall = { sseRemoteDataSource.getAppMode() }
        )

        if (response.responseData?.data?.teamMode != null) {
            if (response.responseData.data.teamMode) {
                sharedPrefs.writeAppMode(response.responseData.data.teamMode)
            }
            return response.responseData.data.teamMode
        }

        return sharedPrefs.isTeamMode()
    }

    private suspend fun <T, A> syncNextBatch(
        lastUpdate: Long,
        dao: BaseDao<T>,
        networkCall: suspend (page: Int) -> Resource<A>,
        saveCallResult: (suspend (A) -> Unit),
        shouldSyncMore: (A) -> Boolean,
        page: Int
    ): Resource<A> {
        val response = performRestOperation(
            networkCall = {
                networkCall(page)
            },
            saveCallResult = {
                saveCallResult(it)
            }
        )

        if (Resource.Status.SUCCESS == response.status) {
            if (response.responseData?.let { shouldSyncMore(it) } == true) {
                syncNextBatch(
                    lastUpdate,
                    dao,
                    networkCall,
                    saveCallResult,
                    shouldSyncMore,
                    page + 1
                )
            } else return response
        } else return response

        return response
    }
}

interface SSERepository : BaseRepository {
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
    suspend fun resetUnreadCount(roomId: Int)
    suspend fun getUnreadCount()
    suspend fun getAppMode(): Boolean
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