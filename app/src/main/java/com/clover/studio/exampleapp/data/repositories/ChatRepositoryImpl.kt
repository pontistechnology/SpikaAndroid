package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.daos.*
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.Note
import com.clover.studio.exampleapp.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.junction.RoomUser
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.NewNote
import com.clover.studio.exampleapp.data.models.networking.responses.MessageResponse
import com.clover.studio.exampleapp.data.models.networking.responses.NotesResponse
import com.clover.studio.exampleapp.data.repositories.data_sources.ChatRemoteDataSource
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.clover.studio.exampleapp.utils.helpers.RestOperations.performRestOperation
import com.clover.studio.exampleapp.utils.helpers.RestOperations.queryDatabase
import com.clover.studio.exampleapp.utils.helpers.RestOperations.queryDatabaseCoreData
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            networkCall = { chatRemoteDataSource.sendMessage(jsonObject) },
            saveCallResult = {
                messageDao.updateMessage(
                    it.data?.message!!.id,
                    it.data.message.fromUserId!!,
                    it.data.message.totalUserCount!!,
                    it.data.message.deliveredCount!!,
                    it.data.message.seenCount!!,
                    it.data.message.type!!,
                    it.data.message.body!!,
                    it.data.message.createdAt!!,
                    it.data.message.modifiedAt!!,
                    it.data.message.deleted!!,
                    it.data.message.replyId ?: 0L,
                    it.data.message.localId!!
                )
            })

    override suspend fun storeMessageLocally(message: Message) {
        queryDatabaseCoreData(
            databaseQuery = { messageDao.upsert(message) }
        )
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

    override suspend fun editMessage(messageId: Int, jsonObject: JsonObject) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.editMessage(messageId, jsonObject) },
            saveCallResult = { messageDao.upsert(it.data?.message!!) }
        )
    }

    override suspend fun updatedRoomVisitedTimestamp(visitedTimestamp: Long, roomId: Int) {
        queryDatabaseCoreData(
            databaseQuery = { roomDao.updateRoomVisited(visitedTimestamp, roomId) }
        )
    }

    override fun getRoomWithUsersLiveData(roomId: Int) =
        queryDatabase(
            databaseQuery = { roomDao.getRoomAndUsersLiveData(roomId) }
        )

    override suspend fun getRoomWithUsers(roomId: Int) =
        queryDatabaseCoreData(
            databaseQuery = { roomDao.getRoomAndUsers(roomId) }
        )

    override suspend fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) {
        val response = performRestOperation(
            networkCall = { chatRemoteDataSource.updateRoom(jsonObject, roomId) })

        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.runInTransaction {
                CoroutineScope(Dispatchers.IO).launch {
                    val oldRoom = roomDao.getRoomById(roomId)

                    response.responseData?.data?.room?.let {
                        queryDatabaseCoreData(
                            databaseQuery = { roomDao.updateRoomTable(oldRoom, it) }
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
                saveCallResult = { roomDao.updateRoomMuted(true, roomId) }
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
                saveCallResult = { roomDao.updateRoomPinned(true, roomId) }
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

    override suspend fun getNotes(roomId: Int) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.getRoomNotes(roomId) },
            saveCallResult = { notesDao.upsert(it.data.notes!!) }
        )
    }

    override fun getLocalNotes(roomId: Int) =
        queryDatabase(
            databaseQuery = { notesDao.getNotesByRoom(roomId) }
        )

    override suspend fun createNewNote(roomId: Int, newNote: NewNote) =
        performRestOperation(
            networkCall = { chatRemoteDataSource.createNewNote(roomId, newNote) },
            saveCallResult = { notesDao.upsert(it.data.note!!) })

    override suspend fun updateNote(noteId: Int, newNote: NewNote) =
        performRestOperation(
            networkCall = { chatRemoteDataSource.updateNote(noteId, newNote) },
            saveCallResult = { notesDao.upsert(it.data.note!!) })

    override suspend fun deleteNote(noteId: Int) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.deleteNote(noteId) },
            saveCallResult = { notesDao.deleteNote(noteId) }
        )
    }

    override suspend fun deleteRoom(roomId: Int) {
        performRestOperation(
            networkCall = { chatRemoteDataSource.deleteRoom(roomId) },
            saveCallResult = { roomDao.deleteRoom(roomId) }
        )
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
}

interface ChatRepository {
    // Message calls
    suspend fun sendMessage(jsonObject: JsonObject): Resource<MessageResponse>
    suspend fun storeMessageLocally(message: Message)
    suspend fun deleteLocalMessages(messages: List<Message>)
    suspend fun deleteLocalMessage(message: Message)
    suspend fun sendMessagesSeen(roomId: Int)
    suspend fun deleteMessage(messageId: Int, target: String)
    suspend fun editMessage(messageId: Int, jsonObject: JsonObject)

    // Room calls
    suspend fun updatedRoomVisitedTimestamp(visitedTimestamp: Long, roomId: Int)
    fun getRoomWithUsersLiveData(roomId: Int): LiveData<Resource<RoomWithUsers>>
    suspend fun getRoomWithUsers(roomId: Int): Resource<RoomWithUsers>
    suspend fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int)
    suspend fun getRoomUserById(roomId: Int, userId: Int): Boolean?
    suspend fun getSingleRoomData(roomId: Int): Resource<RoomAndMessageAndRecords>
    fun getChatRoomAndMessageAndRecordsById(roomId: Int): LiveData<Resource<RoomAndMessageAndRecords>>
    suspend fun handleRoomMute(roomId: Int, doMute: Boolean)
    suspend fun handleRoomPin(roomId: Int, doPin: Boolean)
    suspend fun deleteRoom(roomId: Int)
    suspend fun leaveRoom(roomId: Int)
    suspend fun removeAdmin(roomId: Int, userId: Int)

    // Reaction calls
    suspend fun sendReaction(jsonObject: JsonObject)

    // Notes calls
    suspend fun getNotes(roomId: Int)
    fun getLocalNotes(roomId: Int): LiveData<Resource<List<Note>>>
    suspend fun createNewNote(roomId: Int, newNote: NewNote): Resource<NotesResponse>
    suspend fun updateNote(noteId: Int, newNote: NewNote): Resource<NotesResponse>
    suspend fun deleteNote(noteId: Int)
}
