package com.clover.studio.exampleapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.data.models.UserAndPhoneUser
import com.clover.studio.exampleapp.data.models.networking.Room
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
    val checkRoomExistsListener = MutableLiveData<Event<MainStates>>()
    val createRoomListener = MutableLiveData<Event<MainStates>>()
    val userPhoneUserListener = MutableLiveData<Event<MainStates>>()

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

    fun getUserAndPhoneUser() = liveData {
        emitSource(repository.getUserAndPhoneUser())
    }
}

sealed class MainStates
object UsersFetched : MainStates()
object UsersError : MainStates()
class RoomCreated(val roomData: Room) : MainStates()
object RoomFailed : MainStates()
class RoomExists(val roomData: Room) : MainStates()
object RoomNotFound : MainStates()