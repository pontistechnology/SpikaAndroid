package com.clover.studio.exampleapp.ui.main.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val messageSendListener = MutableLiveData<Event<ChatStates>>()
    val getMessagesListener = MutableLiveData<Event<ChatStates>>()
    val getMessagesTimestampListener = MutableLiveData<Event<ChatStates>>()
    val sendMessageDeliveredListener = MutableLiveData<Event<ChatStates>>()

    fun sendMessage(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.sendMessage(jsonObject)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            messageSendListener.postValue(Event(ChatStates.MESSAGE_SEND_FAIL))
        }

        messageSendListener.postValue(Event(ChatStates.MESSAGE_SENT))
    }

    fun getMessages(roomId: String) = viewModelScope.launch {
        try {
            repository.getMessages(roomId)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            getMessagesListener.postValue(Event(ChatStates.MESSAGES_FETCH_FAIL))
        }

        getMessagesListener.postValue(Event(ChatStates.MESSAGES_FETCHED))
    }

    fun getMessagesTimestamp(timestamp: Int) = viewModelScope.launch {
        try {
            repository.getMessagesTimestamp(timestamp)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            getMessagesTimestampListener.postValue(Event(ChatStates.MESSAGES_TIMESTAMP_FETCH_FAIL))
        }

        getMessagesTimestampListener.postValue(Event(ChatStates.MESSAGES_TIMESTAMP_FETCHED))
    }

    fun sendMessageDelivered(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.sendMessageDelivered(jsonObject)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            sendMessageDeliveredListener.postValue(Event(ChatStates.MESSAGE_DELIVER_FAIL))
        }

        sendMessageDeliveredListener.postValue(Event(ChatStates.MESSAGE_DELIVERED))
    }
}

enum class ChatStates { MESSAGE_SENT, MESSAGE_SEND_FAIL, MESSAGES_FETCHED, MESSAGES_FETCH_FAIL, MESSAGES_TIMESTAMP_FETCHED, MESSAGES_TIMESTAMP_FETCH_FAIL, MESSAGE_DELIVERED, MESSAGE_DELIVER_FAIL }