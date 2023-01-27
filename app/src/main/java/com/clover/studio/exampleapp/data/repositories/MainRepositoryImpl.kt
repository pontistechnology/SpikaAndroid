package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.AppDatabase
import com.clover.studio.exampleapp.data.daos.ChatRoomDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.entity.ChatRoom
import com.clover.studio.exampleapp.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.entity.UserAndPhoneUser
import com.clover.studio.exampleapp.data.models.junction.RoomUser
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.BlockedId
import com.clover.studio.exampleapp.data.models.networking.responses.*
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao,
    private val chatRoomDao: ChatRoomDao,
    private val appDatabase: AppDatabase,
    private val sharedPrefs: SharedPreferencesRepository
) : MainRepository {

    override suspend fun getUserRooms(): List<ChatRoom>? =
        retrofitService.fetchAllUserRooms(getHeaderMap(sharedPrefs.readToken())).data?.list

    override suspend fun getUserByID(id: Int) =
        userDao.getUserById(id)

    override suspend fun getRoomById(userId: Int) =
        retrofitService.getRoomById(getHeaderMap(sharedPrefs.readToken()), userId)

    override suspend fun getRoomByIdLiveData(roomId: Int): LiveData<ChatRoom> =
        chatRoomDao.getRoomByIdLiveData(roomId)

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

    override suspend fun getUserAndPhoneUser(localId: Int): LiveData<List<UserAndPhoneUser>> =
        userDao.getUserAndPhoneUser(localId)

    override suspend fun getChatRoomAndMessageAndRecords(): LiveData<List<RoomAndMessageAndRecords>> =
        chatRoomDao.getChatRoomAndMessageAndRecords()

    override suspend fun getRoomWithUsersLiveData(roomId: Int): LiveData<RoomWithUsers> =
        chatRoomDao.getRoomAndUsersLiveData(roomId)

    override suspend fun getSingleRoomData(roomId: Int): RoomAndMessageAndRecords =
        chatRoomDao.getSingleRoomData(roomId)

    override suspend fun getRoomWithUsers(roomId: Int): RoomWithUsers =
        chatRoomDao.getRoomAndUsers(roomId)

    override suspend fun updatePushToken(jsonObject: JsonObject) =
        retrofitService.updatePushToken(getHeaderMap(sharedPrefs.readToken()), jsonObject)

    override suspend fun updateUserData(jsonObject: JsonObject): AuthResponse {
        val responseData =
            retrofitService.updateUser(getHeaderMap(sharedPrefs.readToken()), jsonObject)

        userDao.insert(responseData.data.user)
        sharedPrefs.writeUserId(responseData.data.user.id)

        return responseData
    }

    override suspend fun uploadFiles(
        jsonObject: JsonObject
    ) = retrofitService.uploadFiles(getHeaderMap(sharedPrefs.readToken()), jsonObject)

    override suspend fun verifyFile(jsonObject: JsonObject): FileResponse =
        retrofitService.verifyFile(getHeaderMap(sharedPrefs.readToken()), jsonObject)

    override suspend fun updateRoom(
        jsonObject: JsonObject,
        roomId: Int,
        userId: Int
    ): RoomResponse {
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

            CoroutineScope(Dispatchers.IO).launch {
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
        return response
    }

    override suspend fun getUserSettings(): List<Settings> =
        retrofitService.getSettings(getHeaderMap(sharedPrefs.readToken())).data.settings

    override suspend fun getBlockedList() {
        val response =
            retrofitService.getBlockedList(getHeaderMap(sharedPrefs.readToken())).data.blockedUsers
        val userIds = response?.map { it.id }
        sharedPrefs.writeBlockedUsersIds(userIds!!)
    }

    override suspend fun fetchBlockedUsersLocally(userIds: List<Int>): List<User> =
        userDao.getUsersByIds(userIds)

    override suspend fun blockUser(blockedId: Int) {
        val response =
            retrofitService.blockUser(getHeaderMap(sharedPrefs.readToken()), BlockedId(blockedId))
        if (Const.JsonFields.SUCCESS == response.status) {
            val currentList: MutableList<Int> =
                sharedPrefs.readBlockedUserList() as MutableList<Int>
            currentList.add(blockedId)
            sharedPrefs.writeBlockedUsersIds(currentList)
        }
    }

    override suspend fun deleteBlock(userId: Int) {
        retrofitService.deleteBlock(getHeaderMap(sharedPrefs.readToken()), userId)
    }

    override suspend fun deleteBlockForSpecificUser(userId: Int) {
        val response = retrofitService.deleteBlockForSpecificUser(
            getHeaderMap(sharedPrefs.readToken()),
            userId
        )
        if (Const.JsonFields.SUCCESS == response.status) {
            val currentList = sharedPrefs.readBlockedUserList()
            val updatedList = currentList.filterNot { it == userId }
            sharedPrefs.writeBlockedUsersIds(updatedList)
        }
    }

    override suspend fun handleRoomMute(roomId: Int, doMute: Boolean) {
        val response: MuteResponse = if (doMute)
            retrofitService.muteRoom(getHeaderMap(sharedPrefs.readToken()), roomId)
        else
            retrofitService.unmuteRoom(getHeaderMap(sharedPrefs.readToken()), roomId)

        if (Const.JsonFields.SUCCESS == response.status) {
            chatRoomDao.updateRoomMuted(true, roomId)
        }
    }

    override suspend fun handleRoomPin(roomId: Int, doPin: Boolean) {
        val response = if (doPin)
            retrofitService.pinRoom(getHeaderMap(sharedPrefs.readToken()), roomId)
        else
            retrofitService.unpinRoom(getHeaderMap(sharedPrefs.readToken()), roomId)

        if (Const.JsonFields.SUCCESS == response.status) {
            chatRoomDao.updateRoomPinned(true, roomId)
        }
    }
}

interface MainRepository {
    suspend fun getUserRooms(): List<ChatRoom>?
    suspend fun getUserByID(id: Int): LiveData<User>
    suspend fun getRoomById(userId: Int): RoomResponse
    suspend fun getRoomByIdLiveData(roomId: Int): LiveData<ChatRoom>
    suspend fun createNewRoom(jsonObject: JsonObject): RoomResponse
    suspend fun getUserAndPhoneUser(localId: Int): LiveData<List<UserAndPhoneUser>>
    suspend fun getChatRoomAndMessageAndRecords(): LiveData<List<RoomAndMessageAndRecords>>
    suspend fun getRoomWithUsersLiveData(roomId: Int): LiveData<RoomWithUsers>
    suspend fun getSingleRoomData(roomId: Int): RoomAndMessageAndRecords
    suspend fun getRoomWithUsers(roomId: Int): RoomWithUsers
    suspend fun updatePushToken(jsonObject: JsonObject)
    suspend fun updateUserData(jsonObject: JsonObject): AuthResponse
    suspend fun uploadFiles(jsonObject: JsonObject): FileResponse
    suspend fun verifyFile(jsonObject: JsonObject): FileResponse
    suspend fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int): RoomResponse
    suspend fun getUserSettings(): List<Settings>
    suspend fun handleRoomMute(roomId: Int, doMute: Boolean)
    suspend fun handleRoomPin(roomId: Int, doPin: Boolean)

    // Block
    suspend fun getBlockedList()
    suspend fun fetchBlockedUsersLocally(userIds: List<Int>): List<User>
    suspend fun blockUser(blockedId: Int)
    suspend fun deleteBlock(userId: Int)
    suspend fun deleteBlockForSpecificUser(userId: Int)
}