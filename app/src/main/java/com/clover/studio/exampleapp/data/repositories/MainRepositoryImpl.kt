package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.daos.MessageRecordsDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.ChatRoomAndMessage
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.models.UserAndPhoneUser
import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.data.models.networking.FileResponse
import com.clover.studio.exampleapp.data.models.networking.RoomResponse
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val messageRecordsDao: MessageRecordsDao,
    private val chatRoomDao: ChatRoomDao,
    private val sharedPrefs: SharedPreferencesRepository
) : MainRepository {
    override suspend fun getUsers() {
        val userData = retrofitService.getUsers(getHeaderMap(sharedPrefs.readToken()))

        if (userData.data?.list != null) {
            for (user in userData.data.list) {
                userDao.insert(user)
            }
        }
    }

    override fun getUserByID(id: Int) =
        userDao.getUserById(id)

    override fun getUserLiveData(): LiveData<List<User>> =
        userDao.getUsers()

    override suspend fun getRoomById(userId: Int) =
        retrofitService.getRoomById(getHeaderMap(sharedPrefs.readToken()), userId)

    override suspend fun getRooms() {
        val roomData = retrofitService.getRooms(getHeaderMap(sharedPrefs.readToken()))

        if (roomData.data?.list != null) {
            for (room in roomData.data.list) {
                chatRoomDao.insert(room)
            }
        }
    }

    override suspend fun getMessages() {
        val roomIds: MutableList<Int> = ArrayList()
        withContext(Dispatchers.IO) {
            chatRoomDao.getRoomsLocally().forEach { roomIds.add(it.roomId) }
        }

        if (roomIds.isNotEmpty()) {
            for (id in roomIds) {
                val messageData = retrofitService.getMessages(
                    getHeaderMap(sharedPrefs.readToken()),
                    id.toString()
                )

                if (messageData.data?.list != null) {
                    for (message in messageData.data.list) {
                        messageDao.insert(message)
                    }
                }
            }
        }
    }

    override suspend fun getRoomsLiveData(): LiveData<List<ChatRoom>> =
        chatRoomDao.getRooms()


    override suspend fun createNewRoom(jsonObject: JsonObject) =
        retrofitService.createNewRoom(getHeaderMap(sharedPrefs.readToken()), jsonObject)

    override suspend fun getUserAndPhoneUser(): LiveData<List<UserAndPhoneUser>> =
        userDao.getUserAndPhoneUser()

    override suspend fun getChatRoomAndMessage(): LiveData<List<ChatRoomAndMessage>> =
        chatRoomDao.getChatRoomAndMessage()

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
        withContext(Dispatchers.IO) {
            messageDao.getMessagesLocally().forEach { messageIds.add(it.id) }
        }

        if (messageIds.isNotEmpty()) {
            for (messageId in messageIds) {
                val recordsData = retrofitService.getMessageRecords(
                    getHeaderMap(sharedPrefs.readToken()),
                    messageId.toString()
                )

                if (recordsData.data.messageRecords.isNotEmpty()) {
                    for (messageRecord in recordsData.data.messageRecords) {
                        messageRecordsDao.insert(messageRecord)
                    }
                }
            }
        }
    }
}

interface MainRepository {
    suspend fun getUsers()
    fun getUserByID(id: Int): LiveData<User>
    fun getUserLiveData(): LiveData<List<User>>
    suspend fun getRoomById(userId: Int): RoomResponse
    suspend fun getRooms()
    suspend fun getMessages()
    suspend fun getRoomsLiveData(): LiveData<List<ChatRoom>>
    suspend fun createNewRoom(jsonObject: JsonObject): RoomResponse
    suspend fun getUserAndPhoneUser(): LiveData<List<UserAndPhoneUser>>
    suspend fun getChatRoomAndMessage(): LiveData<List<ChatRoomAndMessage>>
    suspend fun updatePushToken(jsonObject: JsonObject)
    suspend fun updateUserData(data: Map<String, String>): AuthResponse
    suspend fun uploadFiles(jsonObject: JsonObject): FileResponse
    suspend fun verifyFile(jsonObject: JsonObject): FileResponse
    suspend fun getMessageRecords()
}