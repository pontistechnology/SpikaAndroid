package com.clover.studio.exampleapp.ui.main.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.repositories.ChatRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.Event
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepositoryImpl,
    private val sharedPrefs: SharedPreferencesRepository
) : ViewModel() {
    val messageSendListener = MutableLiveData<Event<ChatStatesEnum>>()
    val getMessagesListener = MutableLiveData<Event<ChatStates>>()
    val getMessagesTimestampListener = MutableLiveData<Event<ChatStates>>()
    val sendMessageDeliveredListener = MutableLiveData<Event<ChatStatesEnum>>()

    fun sendMessage(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.sendMessage(jsonObject)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            messageSendListener.postValue(Event(ChatStatesEnum.MESSAGE_SEND_FAIL))
            return@launch
        }

        messageSendListener.postValue(Event(ChatStatesEnum.MESSAGE_SENT))
    }

    fun getMessages(roomId: Int) = viewModelScope.launch {
        // TODO Write this to return live data, save message data from response to local db
        try {
            val messages = repository.getMessages(roomId.toString()).data?.list
            getMessagesListener.postValue(Event(MessagesFetched(messages!!)))
        } catch (ex: Exception) {
            Tools.checkError(ex)
            getMessagesListener.postValue(Event(MessageFetchFail))
            return@launch
        }
    }

    fun getMessagesTimestamp(timestamp: Int) = viewModelScope.launch {
        try {
            val messages = repository.getMessagesTimestamp(timestamp).data?.list
            getMessagesTimestampListener.postValue(Event(MessagesTimestampFetched(messages!!)))
        } catch (ex: Exception) {
            Tools.checkError(ex)
            getMessagesTimestampListener.postValue(Event(MessageTimestampFetchFail))
        }
    }

    fun sendMessageDelivered(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.sendMessageDelivered(jsonObject)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            sendMessageDeliveredListener.postValue(Event(ChatStatesEnum.MESSAGE_DELIVER_FAIL))
            return@launch
        }

        sendMessageDeliveredListener.postValue(Event(ChatStatesEnum.MESSAGE_DELIVERED))
    }

    fun getLocalUserId(): Int? {
        var userId: Int? = null

        viewModelScope.launch {
            userId = sharedPrefs.readUserId()!!
        }
        return userId
    }
}

sealed class ChatStates
data class MessagesFetched(val messages: List<Message>) : ChatStates()
data class MessagesTimestampFetched(val messages: List<Message>) : ChatStates()
object MessageFetchFail : ChatStates()
object MessageTimestampFetchFail : ChatStates()

enum class ChatStatesEnum { MESSAGE_SENT, MESSAGE_SEND_FAIL, MESSAGE_DELIVERED, MESSAGE_DELIVER_FAIL }