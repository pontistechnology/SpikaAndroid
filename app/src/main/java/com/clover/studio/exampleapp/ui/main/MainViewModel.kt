package com.clover.studio.exampleapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.data.repositories.UserRepositoryImpl
import com.clover.studio.exampleapp.utils.Event
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepositoryImpl,
    private val sharedPrefsRepo: SharedPreferencesRepository
) : ViewModel() {

    val usersListener = MutableLiveData<Event<MainStates>>()
    val checkRoomExistsListener = MutableLiveData<Event<MainStatesEnum>>()
    val createRoomListener = MutableLiveData<Event<MainStates>>()

    fun getContacts() = viewModelScope.launch {
        try {
            repository.getUsers()
            usersListener.postValue(Event(UsersFetched))
        } catch (ex: Exception) {
            Tools.checkError(ex)
            usersListener.postValue(Event(UsersError))
            return@launch
        }
    }

    fun getLocalUser() = liveData {
        val localUserId = sharedPrefsRepo.readUserId()

        if (localUserId != 0) {
            emitSource(repository.getUserByID(localUserId!!))
        } else {
            return@liveData
        }
    }

    fun getLocalUsers() = liveData {
        emitSource(repository.getUserLiveData())
    }

    fun checkIfRoomExists(userId: Int) = viewModelScope.launch {
        try {
            repository.getRoomById(userId)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            checkRoomExistsListener.postValue(Event(MainStatesEnum.ROOM_NOT_FOUND))
            return@launch
        }

        checkRoomExistsListener.postValue(Event(MainStatesEnum.ROOM_EXISTS))
    }

    fun createNewRoom(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            repository.createNewRoom(jsonObject)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            createRoomListener.postValue(Event(RoomFailed))
            return@launch
        }

        createRoomListener.postValue(Event(RoomCreated))
    }
}

sealed class MainStates
object UsersFetched : MainStates()
object UsersError : MainStates()
object RoomCreated : MainStates()
object RoomFailed : MainStates()

enum class MainStatesEnum { ROOM_EXISTS, ROOM_NOT_FOUND }