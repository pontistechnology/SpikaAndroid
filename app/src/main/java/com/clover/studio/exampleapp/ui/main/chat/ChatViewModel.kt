package com.clover.studio.exampleapp.ui.main.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.BaseViewModel
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.models.networking.Settings
import com.clover.studio.exampleapp.data.repositories.ChatRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.ui.main.MainStates
import com.clover.studio.exampleapp.ui.main.SingleRoomData
import com.clover.studio.exampleapp.ui.main.SingleRoomFetchFailed
import com.clover.studio.exampleapp.utils.Event
import com.clover.studio.exampleapp.utils.SSEListener
import com.clover.studio.exampleapp.utils.SSEManager
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepositoryImpl,
    private val sharedPrefs: SharedPreferencesRepository,
    private val sseManager: SSEManager
) : BaseViewModel() {
    val messageSendListener = MutableLiveData<Event<ChatStatesEnum>>()
    val getMessagesListener = MutableLiveData<Event<ChatStates>>()
    val getMessagesTimestampListener = MutableLiveData<Event<ChatStates>>()
    val sendMessageDeliveredListener = MutableLiveData<Event<ChatStatesEnum>>()
    val roomWithUsersListener = MutableLiveData<Event<ChatStates>>()
    val roomDataListener = MutableLiveData<Event<MainStates>>()
    val userSettingsListener = MutableLiveData<Event<ChatStates>>()
    val roomNotificationListener = MutableLiveData<Event<ChatStates>>()

    fun storeMessageLocally(message: Message) = viewModelScope.launch {
        try {
            repository.storeMessageLocally(message)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            return@launch
        }
    }

    fun sendMessage(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.sendMessage(jsonObject)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                messageSendListener.postValue(Event(ChatStatesEnum.MESSAGE_SEND_FAIL))
            }
            return@launch
        }

        messageSendListener.postValue(Event(ChatStatesEnum.MESSAGE_SENT))
    }

    fun getLocalMessages(roomId: Int) = liveData {
        emitSource(repository.getMessagesLiveData(roomId))
    }

    fun getLocalUserId(): Int? {
        var userId: Int? = null

        viewModelScope.launch {
            userId = sharedPrefs.readUserId()
        }

        return userId
    }

    fun deleteLocalMessages(messages: List<Message>) = viewModelScope.launch {
        try {
            repository.deleteLocalMessages(messages)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            return@launch
        }
    }

    fun sendMessagesSeen(roomId: Int) = viewModelScope.launch {
        try {
            repository.sendMessagesSeen(roomId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun updateRoomVisitedTimestamp(chatRoom: ChatRoom) = viewModelScope.launch {
        try {
            repository.updatedRoomVisitedTimestamp(chatRoom)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) = viewModelScope.launch {
        try {
            repository.updateRoom(jsonObject, roomId, userId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun isUserAdmin(roomId: Int, userId: Int): Boolean {
        var isAdmin = false

        runBlocking {
            try {
                isAdmin = repository.getRoomUserById(roomId, userId) == true
            } catch (ex: Exception) {
                Tools.checkError(ex)
            }
        }

        return isAdmin
    }

    fun getRoomAndUsers(roomId: Int) = liveData {
        emitSource(repository.getRoomWithUsersLiveData(roomId))
    }

    fun getPushNotificationStream(listener: SSEListener): Flow<Message> = flow {
        viewModelScope.launch {
            try {
                sseManager.startSSEStream(listener)
            } catch (ex: Exception) {
                Tools.checkError(ex)
                return@launch
            }
        }
    }

    fun getChatRoomAndMessageAndRecordsById(roomId: Int) = liveData {
        emitSource(repository.getChatRoomAndMessageAndRecordsById(roomId))
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

    fun muteRoom(roomId: Int) = viewModelScope.launch {
        try {
            repository.muteRoom(roomId)
        } catch (ex: Exception) {
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            }
            return@launch
        }
    }

    fun unmuteRoom(roomId: Int) = viewModelScope.launch {
        try {
            repository.unmuteRoom(roomId)
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

    fun sendReaction(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.sendReaction(jsonObject)
        } catch (ex: Exception){
            if (Tools.checkError(ex)) {
                setTokenExpiredTrue()
            } else {
                Timber.d("Exception: $ex")
            }
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

sealed class ChatStates
object MessagesFetched : ChatStates()
data class MessagesTimestampFetched(val messages: List<Message>) : ChatStates()
object MessageFetchFail : ChatStates()
object MessageTimestampFetchFail : ChatStates()
class RoomWithUsersFetched(val roomWithUsers: RoomWithUsers) : ChatStates()
object RoomWithUsersFailed : ChatStates()
class RoomNotificationData(val roomWithUsers: RoomWithUsers, val message: Message) : ChatStates()
class UserSettingsFetched(val settings: List<Settings>) : ChatStates()
object UserSettingsFetchFailed : ChatStates()
class SingleRoomData(val roomData: RoomAndMessageAndRecords) : ChatStates()
object SingleRoomFetchFailed : ChatStates()

enum class ChatStatesEnum { MESSAGE_SENT, MESSAGE_SEND_FAIL, MESSAGE_DELIVERED, MESSAGE_DELIVER_FAIL }