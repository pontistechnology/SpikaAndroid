package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.ChatRoomAndMessage
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.models.UserAndPhoneUser
import com.clover.studio.exampleapp.data.models.networking.RoomResponse
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.JsonObject
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao,
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
        retrofitService.getRooms(getHeaderMap(sharedPrefs.readToken()))
    }

    override suspend fun getRoomsLiveData(): LiveData<List<ChatRoom>> =
        chatRoomDao.getRooms()


    override suspend fun createNewRoom(jsonObject: JsonObject) =
        retrofitService.createNewRoom(getHeaderMap(sharedPrefs.readToken()), jsonObject)

    override suspend fun getUserAndPhoneUser(): LiveData<List<UserAndPhoneUser>> =
        userDao.getUserAndPhoneUser()

    override suspend fun getChatRoomAndMessage(): LiveData<List<ChatRoomAndMessage>> =
        chatRoomDao.getChatRoomAndMessage()
}

interface MainRepository {
    suspend fun getUsers()
    fun getUserByID(id: Int): LiveData<User>
    fun getUserLiveData(): LiveData<List<User>>
    suspend fun getRoomById(userId: Int): RoomResponse
    suspend fun getRooms()
    suspend fun getRoomsLiveData(): LiveData<List<ChatRoom>>
    suspend fun createNewRoom(jsonObject: JsonObject): RoomResponse
    suspend fun getUserAndPhoneUser(): LiveData<List<UserAndPhoneUser>>
    suspend fun getChatRoomAndMessage(): LiveData<List<ChatRoomAndMessage>>
}