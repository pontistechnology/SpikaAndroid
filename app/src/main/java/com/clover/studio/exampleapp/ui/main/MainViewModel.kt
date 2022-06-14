package com.clover.studio.exampleapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.repositories.MainRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.Event
import com.clover.studio.exampleapp.utils.SSEManager
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepositoryImpl,
    private val sharedPrefsRepo: SharedPreferencesRepository,
    private val sseManager: SSEManager
) : ViewModel() {

    val usersListener = MutableLiveData<Event<MainStates>>()
    val roomsListener = MutableLiveData<Event<MainStates>>()
    val checkRoomExistsListener = MutableLiveData<Event<MainStates>>()
    val createRoomListener = MutableLiveData<Event<MainStates>>()
    val userUpdateListener = MutableLiveData<Event<MainStates>>()
    val roomWithUsersListener = MutableLiveData<Event<MainStates>>()

    fun getContacts(page: Int) = viewModelScope.launch {
        try {
            val response = repository.getUsers(page)
            usersListener.postValue(Event(UsersFetched(response.data?.list!!)))
        } catch (ex: Exception) {
            Tools.checkError(ex)
            usersListener.postValue(Event(UsersError))
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
        try {
            repository.getRooms()
            roomsListener.postValue(Event(RoomsFetched))
        } catch (ex: Exception) {
            Tools.checkError(ex)
            roomsListener.postValue(Event(RoomFetchFail))
        }
    }

    fun checkIfRoomExists(userId: Int) = viewModelScope.launch {
        try {
            val roomData = repository.getRoomById(userId).data?.room
            checkRoomExistsListener.postValue(Event(RoomExists(roomData!!)))
        } catch (ex: Exception) {
            Tools.checkError(ex)
            checkRoomExistsListener.postValue(Event(RoomNotFound))
            return@launch
        }
    }

    fun createNewRoom(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            val roomData = repository.createNewRoom(jsonObject).data?.room
            createRoomListener.postValue(Event(RoomCreated(roomData!!)))
        } catch (ex: Exception) {
            Tools.checkError(ex)
            createRoomListener.postValue(Event(RoomFailed))
            return@launch
        }
    }

    fun getPushNotificationStream(): Flow<Message> = flow {
        viewModelScope.launch {
            try {
                sseManager.startSSEStream()
            } catch (ex: Exception) {
                Tools.checkError(ex)
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

    fun getRoomWithUsers(roomId: Int) = viewModelScope.launch {
        try {
            val response = repository.getRoomWithUsers(roomId)
            roomWithUsersListener.postValue(Event(RoomWithUsersFetched(response)))
        } catch (ex: Exception) {
            Tools.checkError(ex)
            return@launch
        }
    }

    fun updatePushToken(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.updatePushToken(jsonObject)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            return@launch
        }
    }

    fun updateUserData(userMap: HashMap<String, String>) = viewModelScope.launch {
        try {
            repository.updateUserData(userMap)
            sharedPrefsRepo.accountCreated(true)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            userUpdateListener.postValue(Event(UserUpdateFailed))
            return@launch
        }

        userUpdateListener.postValue(Event(UserUpdated))
    }
}

sealed class MainStates
class UsersFetched(val userCount: List<User>) : MainStates()
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
