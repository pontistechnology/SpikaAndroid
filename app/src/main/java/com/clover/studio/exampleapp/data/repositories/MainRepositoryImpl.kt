package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.daos.MessageRecordsDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.*
import com.clover.studio.exampleapp.data.models.junction.RoomUser
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.*
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val messageRecordsDao: MessageRecordsDao,
    private val chatRoomDao: ChatRoomDao,
    private val appDatabase: AppDatabase,
    private val sharedPrefs: SharedPreferencesRepository
) : MainRepository {
    override suspend fun getUsers(page: Int): ContactResponse {
        val userData = retrofitService.getUsers(getHeaderMap(sharedPrefs.readToken()), page)

        val users: MutableList<User> = ArrayList()
        if (userData.data?.list != null) {
            for (user in userData.data.list) {
                users.add(user)
            }
        }
        userDao.insert(users)
        return userData
    }

    override suspend fun getUserByID(id: Int) =
        userDao.getUserById(id)

    override suspend fun getUserLiveData(): LiveData<List<User>> =
        userDao.getUsers()

    override suspend fun getRoomById(userId: Int) =
        retrofitService.getRoomById(getHeaderMap(sharedPrefs.readToken()), userId)

    override suspend fun getRooms(page: Int): RoomResponse {
        val roomData = retrofitService.getRooms(getHeaderMap(sharedPrefs.readToken()), page)

        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.runInTransaction {
                CoroutineScope(Dispatchers.IO).launch {
                    if (roomData.data?.list != null) {
                        val users: MutableList<User> = ArrayList()
                        val roomUsers: MutableList<RoomUser> = ArrayList()
                        val chatRooms: MutableList<ChatRoomUpdate> = ArrayList()
                        for (room in roomData.data.list) {
                            val oldData = chatRoomDao.getRoomById(room.roomId)
                            chatRooms.add(ChatRoomUpdate(oldData, room))

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
                    }
                }
            }
        }
        return roomData
    }

    override suspend fun getMessages() {
        val roomIds: MutableList<Int> = ArrayList()
        chatRoomDao.getRoomsLocally().forEach { roomIds.add(it.roomId) }

        if (roomIds.isNotEmpty()) {
            val messages: MutableList<Message> = ArrayList()
            for (id in roomIds) {
                val messageData = retrofitService.getMessages(
                    getHeaderMap(sharedPrefs.readToken()),
                    id.toString()
                )

                if (messageData.data?.messages != null) {
                    for (message in messageData.data.messages) {
                        messages.add(message)
                    }
                }
            }
            messageDao.insert(messages)
        }
    }

    override suspend fun getRoomsLiveData(): LiveData<List<ChatRoom>> =
        chatRoomDao.getRooms()

    override suspend fun createNewRoom(jsonObject: JsonObject): RoomResponse {
        val response =
            retrofitService.createNewRoom(getHeaderMap(sharedPrefs.readToken()), jsonObject)

        val oldRoom = response.data?.room?.roomId?.let { chatRoomDao.getRoomById(it) }
        response.data?.room?.let { chatRoomDao.updateRoomTable(oldRoom, it) }

        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.runInTransaction {
                CoroutineScope(Dispatchers.IO).launch {
                    val users: MutableList<User> = ArrayList()
                    val roomUsers: MutableList<RoomUser> = ArrayList()
                    for (user in response.data?.room?.users!!) {
                        user.user?.let { users.add(it) }
                        roomUsers.add(
                            RoomUser(
                                response.data.room.roomId,
                                user.userId,
                                user.isAdmin
                            )
                        )
                    }
                    chatRoomDao.insertRoomWithUsers(roomUsers)
                    userDao.insert(users)
                }
            }
        }
        return response
    }

    override suspend fun getUserAndPhoneUser(): LiveData<List<UserAndPhoneUser>> =
        userDao.getUserAndPhoneUser()

    override suspend fun getChatRoomAndMessageAndRecords(): LiveData<List<RoomAndMessageAndRecords>> =
        chatRoomDao.getChatRoomAndMessageAndRecords()

    override suspend fun getSingleRoomData(roomId: Int): RoomAndMessageAndRecords =
        chatRoomDao.getSingleRoomData(roomId)

    override suspend fun getRoomWithUsers(roomId: Int): RoomWithUsers =
        chatRoomDao.getRoomAndUsers(roomId)

    override suspend fun updatePushToken(jsonObject: JsonObject) =
        retrofitService.updatePushToken(getHeaderMap(sharedPrefs.readToken()), jsonObject)

    override suspend fun updateUserData(data: Map<String, String>): AuthResponse {
        val responseData =
            retrofitService.updateUser(getHeaderMap(sharedPrefs.readToken()), data)

        userDao.insert(responseData.data.user)
        sharedPrefs.writeUserId(responseData.data.user.id)

        return responseData
    }

    override suspend fun uploadFiles(
        jsonObject: JsonObject
    ) = retrofitService.uploadFiles(getHeaderMap(sharedPrefs.readToken()), jsonObject)

    override suspend fun verifyFile(jsonObject: JsonObject): FileResponse =
        retrofitService.verifyFile(getHeaderMap(sharedPrefs.readToken()), jsonObject)

    override suspend fun getMessageRecords() {
        val messageIds: MutableList<Int> = ArrayList()
        messageDao.getMessagesLocally().forEach { messageIds.add(it.id) }

        val messageRecords: MutableList<MessageRecords> = ArrayList()
        if (messageIds.isNotEmpty()) {
            for (messageId in messageIds) {
                val recordsData = retrofitService.getMessageRecords(
                    getHeaderMap(sharedPrefs.readToken()),
                    messageId.toString()
                )

                if (recordsData.data.messageRecords.isNotEmpty()) {
                    for (messageRecord in recordsData.data.messageRecords) {
                        messageRecords.add(messageRecord)
                    }
                }
            }
            messageRecordsDao.insert(messageRecords)
        }
    }

    override suspend fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) {
        val response =
            retrofitService.updateRoom(getHeaderMap(sharedPrefs.readToken()), jsonObject, roomId)

        val oldRoom = chatRoomDao.getRoomById(roomId)
        response.data?.room?.let { chatRoomDao.updateRoomTable(oldRoom, it) }

        if (response.data?.room != null) {
            val room = response.data.room

            // Delete Room User if id has been passed through
            if (userId != 0) {
                chatRoomDao.deleteRoomUser(RoomUser(roomId, userId, false))
            }

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
    }

    override suspend fun getUserSettings(): List<Settings> =
        retrofitService.getSettings(getHeaderMap(sharedPrefs.readToken())).data.settings

    override suspend fun deleteRoom(roomId: Int) =
        retrofitService.deleteRoom(getHeaderMap(sharedPrefs.readToken()), roomId)
}

interface MainRepository {
    suspend fun getUsers(page: Int): ContactResponse
    suspend fun getUserByID(id: Int): LiveData<User>
    suspend fun getUserLiveData(): LiveData<List<User>>
    suspend fun getRoomById(userId: Int): RoomResponse
    suspend fun getRooms(page: Int): RoomResponse
    suspend fun getMessages()
    suspend fun getRoomsLiveData(): LiveData<List<ChatRoom>>
    suspend fun createNewRoom(jsonObject: JsonObject): RoomResponse
    suspend fun getUserAndPhoneUser(): LiveData<List<UserAndPhoneUser>>
    suspend fun getChatRoomAndMessageAndRecords(): LiveData<List<RoomAndMessageAndRecords>>
    suspend fun getSingleRoomData(roomId: Int): RoomAndMessageAndRecords
    suspend fun getRoomWithUsers(roomId: Int): RoomWithUsers
    suspend fun updatePushToken(jsonObject: JsonObject)
    suspend fun updateUserData(data: Map<String, String>): AuthResponse
    suspend fun uploadFiles(jsonObject: JsonObject): FileResponse
    suspend fun verifyFile(jsonObject: JsonObject): FileResponse
    suspend fun getMessageRecords()
    suspend fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int)
    suspend fun getUserSettings(): List<Settings>
    suspend fun deleteRoom(roomId: Int)
}