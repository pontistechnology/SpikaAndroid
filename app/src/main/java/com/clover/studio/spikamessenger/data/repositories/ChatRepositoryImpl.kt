package com.clover.studio.spikamessenger.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.spikamessenger.data.AppDatabase
import com.clover.studio.spikamessenger.data.daos.ChatRoomDao
import com.clover.studio.spikamessenger.data.daos.MessageDao
import com.clover.studio.spikamessenger.data.daos.NotesDao
import com.clover.studio.spikamessenger.data.daos.RoomUserDao
import com.clover.studio.spikamessenger.data.daos.UserDao
import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.Note
import com.clover.studio.spikamessenger.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.junction.RoomUser
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.data.models.networking.NewNote
import com.clover.studio.spikamessenger.data.models.networking.responses.MessageResponse
import com.clover.studio.spikamessenger.data.models.networking.responses.NotesResponse
import com.clover.studio.spikamessenger.data.models.networking.responses.RoomResponse
import com.clover.studio.spikamessenger.data.repositories.data_sources.ChatRemoteDataSource
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.clover.studio.spikamessenger.utils.helpers.RestOperations.performRestOperation
import com.clover.studio.spikamessenger.utils.helpers.RestOperations.queryDatabase
import com.clover.studio.spikamessenger.utils.helpers.RestOperations.queryDatabaseCoreData
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatRemoteDataSource: ChatRemoteDataSource,
    private val roomDao: ChatRoomDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val roomUserDao: RoomUserDao,
    private val notesDao: NotesDao,
    private val appDatabase: AppDatabase,
) : ChatRepository {
    override suspend fun sendMessage(jsonObject: JsonObject) =
        performRestOperation(
            networkCall = { chatRemoteDataSource.sendMessage(jsonObject) })

    override suspend fun storeMessageLocally(message: Message) {
        Timber.d("storeMessageLocally")
        Timber.d("Message id: ${message.id}, ${message.localId}, ${message.body}")
        val response = queryDatabaseCoreData(
            databaseQuery = { messageDao.getMessageByLocalId(message.localId.toString()) }
        ).responseData

        if (response == null) {
            messageDao.insert(message)
        }
    }

    override suspend fun deleteLocalMessages(messages: List<Message>) {
        if (messages.isNotEmpty()) {
            val messagesIds = mutableListOf<Long>()
            for (message in messages) {
                messagesIds.add(message.id.toLong())
            }
            queryDatabaseCoreData(
                databaseQuery = { messageDao.deleteMessage(messagesIds) }
            )
        }
    }

    override suspend fun deleteLocalMessage(message: Message) {
        queryDatabaseCoreData(
            databaseQuery = { messageDao.delete(message) }
        )
    }

    override suspend fun sendMessagesSeen(roomId: Int) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.sendMessagesSeen(roomId) })
    }

    override suspend fun deleteMessage(messageId: Int, target: String) {
        val response = performRestOperation(
            networkCall = { chatRemoteDataSource.deleteMessage(messageId, target) })

        if (response.responseData?.data?.message != null) {
            val deletedMessage = response.responseData.data.message
            deletedMessage.type = Const.JsonFields.TEXT_TYPE

            queryDatabaseCoreData(
                databaseQuery = { messageDao.upsert(deletedMessage) }
            )
        }
    }

    override suspend fun updateMessageStatus(messageStatus: String, localId: String) {
        queryDatabaseCoreData(
            databaseQuery = { messageDao.updateMessageStatus(messageStatus, localId) }
        )
    }

    override suspend fun updateLocalUri(localId: String, uri: String) {
        queryDatabaseCoreData(
            databaseQuery = { messageDao.updateLocalUri(localId, uri) }
        )
    }

    override suspend fun editMessage(messageId: Int, jsonObject: JsonObject) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.editMessage(messageId, jsonObject) },
            saveCallResult = { it.data?.message?.let { message -> messageDao.upsert(message) } }
        )
    }

    override fun getMessagesAndRecords(
        roomId: Int,
        limit: Int,
        offset: Int
    ) =
        queryDatabase(
            databaseQuery = { messageDao.getMessagesAndRecords(roomId, limit, offset) }
        )

//    override fun getMessages(roomId: Int) = messageDao.getMessages(roomId)

    override suspend fun getMessageCount(roomId: Int) =
        messageDao.getMessageCount(roomId)

    override fun getRoomWithUsersLiveData(roomId: Int) =
        queryDatabase(
            databaseQuery = { roomDao.getRoomAndUsersLiveData(roomId) }
        )

    override suspend fun getRoomUsers(roomId: Int): RoomWithUsers? =
        queryDatabaseCoreData(
            databaseQuery = { roomDao.getRoomUsers(roomId) }
        ).responseData

    override suspend fun getRoomWithUsers(roomId: Int) =
        queryDatabaseCoreData(
            databaseQuery = { roomDao.getRoomAndUsers(roomId) }
        )

    override suspend fun updateRoom(
        jsonObject: JsonObject,
        roomId: Int,
        userId: Int
    ): Resource<RoomResponse> {
        val response = performRestOperation(
            networkCall = { chatRemoteDataSource.updateRoom(jsonObject, roomId) })

        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.runInTransaction {
                CoroutineScope(Dispatchers.IO).launch {
                    response.responseData?.data?.room?.let {
                        queryDatabaseCoreData(
                            databaseQuery = { roomDao.upsert(it) }
                        )
                    }

                    val users: MutableList<User> = ArrayList()
                    val roomUsers: MutableList<RoomUser> = ArrayList()
                    if (response.responseData?.data?.room != null) {
                        val room = response.responseData.data.room

                        // Delete Room User if id has been passed through
                        if (userId != 0) {
                            queryDatabaseCoreData(
                                databaseQuery = {
                                    roomUserDao.delete(
                                        RoomUser(
                                            roomId,
                                            userId,
                                            false
                                        )
                                    )
                                }
                            )
                        }

                        for (user in room.users) {
                            user.user?.let { users.add(it) }
                            roomUsers.add(
                                RoomUser(
                                    room.roomId, user.userId, user.isAdmin
                                )
                            )
                        }

                        queryDatabaseCoreData(
                            databaseQuery = { userDao.upsert(users) }
                        )

                        queryDatabaseCoreData(
                            databaseQuery = { roomUserDao.upsert(roomUsers) }
                        )
                    }
                }
            }
        }
        return response
    }

    override suspend fun getRoomUserById(roomId: Int, userId: Int): Boolean? =
        queryDatabaseCoreData(
            databaseQuery = { roomUserDao.getRoomUserById(roomId, userId).isAdmin }
        ).responseData


    override fun getChatRoomAndMessageAndRecordsById(roomId: Int) =
        queryDatabase(
            databaseQuery = { roomDao.getDistinctChatRoomAndMessageAndRecordsById(roomId) }
        )

    override suspend fun handleRoomMute(roomId: Int, doMute: Boolean) {
        if (doMute) {
            performRestOperation(
                networkCall = { chatRemoteDataSource.muteRoom(roomId) },
                saveCallResult = { roomDao.updateRoomMuted(true, roomId) }
            )
        } else {
            performRestOperation(
                networkCall = { chatRemoteDataSource.unmuteRoom(roomId) },
                saveCallResult = { roomDao.updateRoomMuted(false, roomId) }
            )
        }
    }

    override suspend fun handleRoomPin(roomId: Int, doPin: Boolean) {
        if (doPin) {
            performRestOperation(
                networkCall = { chatRemoteDataSource.pinRoom(roomId) },
                saveCallResult = { roomDao.updateRoomPinned(true, roomId) }
            )
        } else {
            performRestOperation(
                networkCall = { chatRemoteDataSource.unpinRoom(roomId) },
                saveCallResult = { roomDao.updateRoomPinned(false, roomId) }
            )
        }
    }

    override suspend fun getSingleRoomData(roomId: Int) =
        queryDatabaseCoreData(
            databaseQuery = { roomDao.getSingleRoomData(roomId) }
        )

    override suspend fun sendReaction(jsonObject: JsonObject) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.postReaction(jsonObject) })
    }

    override suspend fun deleteReaction(messageRecordId: Long) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.deleteReaction(messageRecordId) })
    }

    override suspend fun getNotes(roomId: Int) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.getRoomNotes(roomId) },
            saveCallResult = { it.data.notes?.let { notes -> notesDao.upsert(notes) } }
        )
    }

    override fun getLocalNotes(roomId: Int) =
        queryDatabase(
            databaseQuery = { notesDao.getNotesByRoom(roomId) }
        )

    override suspend fun createNewNote(roomId: Int, newNote: NewNote) =
        performRestOperation(
            networkCall = { chatRemoteDataSource.createNewNote(roomId, newNote) },
            saveCallResult = { it.data.note?.let { note -> notesDao.upsert(note) } })

    override suspend fun updateNote(noteId: Int, newNote: NewNote) =
        performRestOperation(
            networkCall = { chatRemoteDataSource.updateNote(noteId, newNote) },
            saveCallResult = { it.data.note?.let { note -> notesDao.upsert(note) } })

    override suspend fun deleteNote(noteId: Int) =
        performRestOperation(
            networkCall = { chatRemoteDataSource.deleteNote(noteId) },
            saveCallResult = { notesDao.deleteNote(noteId) }
        )

    override suspend fun deleteRoom(roomId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            performRestOperation(
                networkCall = { chatRemoteDataSource.deleteRoom(roomId) },
                saveCallResult = { roomDao.updateRoomDeleted(roomId, true) }
            )
        }
    }

    override suspend fun leaveRoom(roomId: Int) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.leaveRoom(roomId) },
            saveCallResult = { roomDao.updateRoomExit(roomId, true) }
        )
    }

    override suspend fun removeAdmin(roomId: Int, userId: Int) {
        queryDatabaseCoreData(
            databaseQuery = { roomUserDao.removeAdmin(roomId, userId) }
        )
    }

    override suspend fun getUnreadCount() {
        val response = performRestOperation(
            networkCall = { chatRemoteDataSource.getUnreadCount() }
        )

        CoroutineScope(Dispatchers.IO).launch {
            if (response.responseData?.data?.unreadCounts != null) {
                val currentRooms = roomDao.getAllRooms()
                val roomsToUpdate: MutableList<ChatRoom> = ArrayList()
                for (room in currentRooms) {
                    room.unreadCount = 0
                    for (item in response.responseData.data.unreadCounts) {
                        if (item.roomId == room.roomId) {
                            room.unreadCount = item.unreadCount
                            break
                        }
                    }
                    roomsToUpdate.add(room)
                }
//                Timber.d("Rooms to update: $roomsToUpdate")
                queryDatabaseCoreData(
                    databaseQuery = { roomDao.upsert(roomsToUpdate) }
                )
            }
        }
    }
}

interface ChatRepository : BaseRepository {
    // Message calls
    suspend fun sendMessage(jsonObject: JsonObject): Resource<MessageResponse>
    suspend fun storeMessageLocally(message: Message)
    suspend fun deleteLocalMessages(messages: List<Message>)
    suspend fun deleteLocalMessage(message: Message)
    suspend fun sendMessagesSeen(roomId: Int)
    suspend fun deleteMessage(messageId: Int, target: String)
    suspend fun editMessage(messageId: Int, jsonObject: JsonObject)
    fun getMessagesAndRecords(
        roomId: Int,
        limit: Int,
        offset: Int
    ): LiveData<Resource<List<MessageAndRecords>>>
//    fun getMessages(roomId: Int) : List<Message>

    suspend fun getMessageCount(roomId: Int): Int
    suspend fun updateMessageStatus(messageStatus: String, localId: String)
    suspend fun updateLocalUri(localId: String, uri: String)

    // Room calls
    fun getRoomWithUsersLiveData(roomId: Int): LiveData<Resource<RoomWithUsers>>
    suspend fun getRoomWithUsers(roomId: Int): Resource<RoomWithUsers>
    suspend fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int): Resource<RoomResponse>
    suspend fun getRoomUserById(roomId: Int, userId: Int): Boolean?
    suspend fun getSingleRoomData(roomId: Int): Resource<RoomAndMessageAndRecords>
    fun getChatRoomAndMessageAndRecordsById(roomId: Int): LiveData<Resource<RoomAndMessageAndRecords>>
    suspend fun handleRoomMute(roomId: Int, doMute: Boolean)
    suspend fun handleRoomPin(roomId: Int, doPin: Boolean)
    suspend fun deleteRoom(roomId: Int)
    suspend fun leaveRoom(roomId: Int)
    suspend fun removeAdmin(roomId: Int, userId: Int)

    suspend fun getRoomUsers(roomId: Int): RoomWithUsers?

    // Reaction calls
    suspend fun sendReaction(jsonObject: JsonObject)
    suspend fun deleteReaction(messageRecordId: Long)

    // Notes calls
    suspend fun getNotes(roomId: Int)
    fun getLocalNotes(roomId: Int): LiveData<Resource<List<Note>>>
    suspend fun createNewNote(roomId: Int, newNote: NewNote): Resource<NotesResponse>
    suspend fun updateNote(noteId: Int, newNote: NewNote): Resource<NotesResponse>
    suspend fun deleteNote(noteId: Int): Resource<NotesResponse>

    suspend fun getUnreadCount()
}
