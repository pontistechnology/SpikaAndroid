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

    fun sendMessage(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.sendMessage(jsonObject)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            messageSendListener.postValue(Event(ChatStates.MESSAGE_SEND_FAIL))
        }

        messageSendListener.postValue(Event(ChatStates.MESSAGE_SENT))
    }
}

enum class ChatStates { MESSAGE_SENT, MESSAGE_SEND_FAIL, MESSAGES_FETCHED, MESSAGE_FETCH_FAIL }