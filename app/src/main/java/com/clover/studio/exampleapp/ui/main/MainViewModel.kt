package com.clover.studio.exampleapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.BaseViewModel
import com.clover.studio.exampleapp.data.models.entity.ChatRoom
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.repositories.MainRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.Event
import com.clover.studio.exampleapp.utils.SSEListener
import com.clover.studio.exampleapp.utils.SSEManager
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepositoryImpl,
    private val sharedPrefsRepo: SharedPreferencesRepository,
    private val sseManager: SSEManager
) : BaseViewModel() {

    val usersListener = MutableLiveData<Event<MainStates>>()
    val checkRoomExistsListener = MutableLiveData<Event<MainStates>>()
    val createRoomListener = MutableLiveData<Event<MainStates>>()
    val userUpdateListener = MutableLiveData<Event<MainStates>>()
    val roomWithUsersListener = MutableLiveData<Event<MainStates>>()
    val roomDataListener = MutableLiveData<Event<MainStates>>()
    val roomNotificationListener = MutableLiveData<Event<MainStates>>()
    val blockedListListener = MutableLiveData<Event<MainStates>>()

    fun getLocalUser() = liveData {
        val localUserId = sharedPrefsRepo.readUserId()

        if (localUserId != null && localUserId != 0) {
            emitSource(repository.getUserByID(localUserId))
        } else {
            return@liveData
        }
    }

    fun getLocalUserId(): Int? {
        var userId: Int? = null

        viewModelScope.launch {
            userId = sharedPrefsRepo.readUserId()
        }
        return userId
    }

    fun checkIfRoomExists(userId: Int) = viewModelScope.launch {
        try {
            val roomData = repository.getRoomById(userId).data?.room
            checkRoomExistsListener.postValue(Event(RoomExists(roomData!!)))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                checkRoomExistsListener.postValue(Event(RoomNotFound))
            }
            return@launch
        }
    }

    fun createNewRoom(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            val roomData = repository.createNewRoom(jsonObject).data?.room
            createRoomListener.postValue(Event(RoomCreated(roomData!!)))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                createRoomListener.postValue(Event(RoomCreateFailed))
            }
            return@launch
        }
    }

    fun getPushNotificationStream(listener: SSEListener): Flow<Message> = flow {
        viewModelScope.launch {
            try {
                sseManager.startSSEStream(listener)
            } catch (ex: Exception) {
                if (Tools.checkError(ex)) {
                    setTokenExpiredTrue()
                }
                return@launch
            }
        }
    }

    fun getUserAndPhoneUser(localId: Int) = liveData {
        emitSource(repository.getUserAndPhoneUser(localId))
    }

    fun getChatRoomAndMessageAndRecords() = liveData {
        emitSource(repository.getChatRoomAndMessageAndRecords())
    }

    fun getRoomByIdLiveData(roomId: Int) = liveData {
        emitSource(repository.getRoomByIdLiveData(roomId))
    }

    fun getSingleRoomData(roomId: Int) = viewModelScope.launch {
        try {
            roomDataListener.postValue(Event(SingleRoomData(repository.getSingleRoomData(roomId))))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                roomDataListener.postValue(Event(SingleRoomFetchFailed))
            }
            return@launch
        }
    }

    fun getRoomWithUsers(roomId: Int) = viewModelScope.launch {
        try {
            val response = repository.getRoomWithUsers(roomId)
            roomWithUsersListener.postValue(Event(RoomWithUsersFetched(response)))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun getRoomWithUsers(roomId: Int, message: Message) = viewModelScope.launch {
        try {
            roomNotificationListener.postValue(
                Event(
                    RoomNotificationData(
                        repository.getRoomWithUsers(
                            roomId
                        ), message
                    )
                )
            )
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                roomNotificationListener.postValue(Event(RoomWithUsersFailed))
            }
            return@launch
        }
    }


    fun updatePushToken(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.updatePushToken(jsonObject)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun updateUserData(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.updateUserData(jsonObject)
            sharedPrefsRepo.accountCreated(true)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                userUpdateListener.postValue(Event(UserUpdateFailed))
            }
            return@launch
        }

        userUpdateListener.postValue(Event(UserUpdated))
    }


    fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) = viewModelScope.launch {
        try {
            Timber.d("RoomDataCalled")
            val roomData = repository.updateRoom(jsonObject, roomId, userId).data?.room
            createRoomListener.postValue(Event(RoomCreated(roomData!!)))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                createRoomListener.postValue(Event(RoomUpdateFailed))
            }
            return@launch
        }
    }

    fun unregisterSharedPrefsReceiver() = viewModelScope.launch {
        sharedPrefsRepo.unregisterSharedPrefsReceiver()
    }

    fun blockedUserListListener() = liveData {
        emitSource(sharedPrefsRepo.blockUserListener())
    }

    fun fetchBlockedUsersLocally(userIds: List<Int>) = viewModelScope.launch {
        try {
            val data = repository.fetchBlockedUsersLocally(userIds)
            blockedListListener.postValue(Event(BlockedUsersFetched(data)))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            blockedListListener.postValue(Event(BlockedUsersFetchFailed))
            return@launch
        }
    }

    fun getBlockedUsersList() = viewModelScope.launch {
        try {
            repository.getBlockedList()
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun blockUser(blockedId: Int) = viewModelScope.launch {
        try {
            repository.blockUser(blockedId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun deleteBlock(userId: Int) = viewModelScope.launch {
        try {
            repository.deleteBlock(userId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun deleteBlockForSpecificUser(userId: Int) = viewModelScope.launch {
        try {
            repository.deleteBlockForSpecificUser(userId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }
}

sealed class MainStates
object UsersFetched : MainStates()
object UsersError : MainStates()
class RoomCreated(val roomData: ChatRoom) : MainStates()
class RoomUpdated(val roomData: ChatRoom) : MainStates()
object RoomCreateFailed : MainStates()
object RoomUpdateFailed : MainStates()
class RoomExists(val roomData: ChatRoom) : MainStates()
object RoomNotFound : MainStates()
object UserUpdated : MainStates()
object UserUpdateFailed : MainStates()
class RoomWithUsersFetched(val roomWithUsers: RoomWithUsers) : MainStates()
object RoomWithUsersFailed : MainStates()
class RoomNotificationData(val roomWithUsers: RoomWithUsers, val message: Message) : MainStates()
class SingleRoomData(val roomData: RoomAndMessageAndRecords) : MainStates()
object SingleRoomFetchFailed : MainStates()
class BlockedUsersFetched(val users: List<User>) : MainStates()
object BlockedUsersFetchFailed : MainStates()
