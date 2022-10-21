package com.clover.studio.exampleapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.BaseViewModel
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.Settings
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
    val roomsListener = MutableLiveData<Event<MainStates>>()
    val checkRoomExistsListener = MutableLiveData<Event<MainStates>>()
    val createRoomListener = MutableLiveData<Event<MainStates>>()
    val userUpdateListener = MutableLiveData<Event<MainStates>>()
    val roomWithUsersListener = MutableLiveData<Event<MainStates>>()
    val roomDataListener = MutableLiveData<Event<MainStates>>()
    val userSettingsListener = MutableLiveData<Event<MainStates>>()
    val roomNotificationListener = MutableLiveData<Event<MainStates>>()

    fun getContacts() = viewModelScope.launch {
        var page = 1
        try {
            var count: Double? = repository.getUsers(page).data?.count?.toDouble()
            if (count != null) {
                while (count!! / 10 > page) {
                    page++
                    count = repository.getUsers(page).data?.count?.toDouble()
                    Timber.d("Count = $count")
                }
                usersListener.postValue(Event(UsersFetched))
            }
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                usersListener.postValue(Event(UsersError))
            }
            return@launch
        }
    }

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

    fun getRooms() = viewModelScope.launch {
        var page = 1
        try {
            var count = repository.getRooms(page).data?.count?.toDouble()
            if (count != null) {
                while (count!! / 10 > page) {
                    page++
                    count = repository.getRooms(page).data?.count?.toDouble()
                    Timber.d("Count = $count")
                }
                roomsListener.postValue(Event(RoomsFetched))
            }
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                roomsListener.postValue(Event(RoomFetchFail))
            }
        }
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
                createRoomListener.postValue(Event(RoomFailed))
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

    fun getUserAndPhoneUser() = liveData {
        emitSource(repository.getUserAndPhoneUser())
    }

    fun getChatRoomAndMessageAndRecords() = liveData {
        emitSource(repository.getChatRoomAndMessageAndRecords())
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

    fun updateUserData(userMap: HashMap<String, String>) = viewModelScope.launch {
        try {
            repository.updateUserData(userMap)
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
            repository.updateRoom(jsonObject, roomId, userId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun getUserSettings() = viewModelScope.launch {
        try {
            val data = repository.getUserSettings()
            userSettingsListener.postValue(Event(UserSettingsFetched(data)))
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                userSettingsListener.postValue(Event(UserSettingsFetchFailed))
            }
            return@launch
        }
    }

    fun deleteRoom(roomId: Int) = viewModelScope.launch {
        try {
            repository.deleteRoom(roomId)
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
object RoomsFetched : MainStates()
object RoomFetchFail : MainStates()
class RoomCreated(val roomData: ChatRoom) : MainStates()
object RoomFailed : MainStates()
class RoomExists(val roomData: ChatRoom) : MainStates()
object RoomNotFound : MainStates()
object UserUpdated : MainStates()
object UserUpdateFailed : MainStates()
class RoomWithUsersFetched(val roomWithUsers: RoomWithUsers) : MainStates()
object RoomWithUsersFailed : MainStates()
class RoomNotificationData(val roomWithUsers: RoomWithUsers, val message: Message) : MainStates()
class SingleRoomData(val roomData: RoomAndMessageAndRecords) : MainStates()
object SingleRoomFetchFailed : MainStates()
class UserSettingsFetched(val settings: List<Settings>) : MainStates()
object UserSettingsFetchFailed : MainStates()
