package com.clover.studio.exampleapp.ui.main.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.BaseViewModel
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.repositories.ChatRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.Event
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepositoryImpl,
    private val sharedPrefs: SharedPreferencesRepository
) : BaseViewModel() {
    val messageSendListener = MutableLiveData<Event<ChatStatesEnum>>()
    val getMessagesListener = MutableLiveData<Event<ChatStates>>()
    val getMessagesTimestampListener = MutableLiveData<Event<ChatStates>>()
    val sendMessageDeliveredListener = MutableLiveData<Event<ChatStatesEnum>>()
    val roomWithUsersListener = MutableLiveData<Event<ChatStates>>()

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
                messageSendListener.postValue(Event(ChatStatesEnum.TOKEN_EXPIRED))
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
            Tools.checkError(ex)
            return@launch
        }
    }

    fun updateRoomVisitedTimestamp(chatRoom: ChatRoom) = viewModelScope.launch {
        try {
            repository.updatedRoomVisitedTimestamp(chatRoom)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            return@launch
        }
    }

    fun updateRoom(jsonObject: JsonObject, roomId: Int, userId: Int) = viewModelScope.launch {
        try {
            repository.updateRoom(jsonObject, roomId, userId)
        } catch (ex: Exception) {
            Tools.checkError(ex)
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
        emitSource(repository.getRoomWithUsers(roomId))
    }
}

sealed class ChatStates
object MessagesFetched : ChatStates()
data class MessagesTimestampFetched(val messages: List<Message>) : ChatStates()
object MessageFetchFail : ChatStates()
object MessageTimestampFetchFail : ChatStates()
class RoomWithUsersFetched(val roomWithUsers: RoomWithUsers) : ChatStates()
object RoomWithUsersFailed : ChatStates()

enum class ChatStatesEnum { MESSAGE_SENT, MESSAGE_SEND_FAIL, MESSAGE_DELIVERED, MESSAGE_DELIVER_FAIL, TOKEN_EXPIRED }