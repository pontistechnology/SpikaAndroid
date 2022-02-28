package com.clover.studio.exampleapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.data.repositories.UserRepositoryImpl
import com.clover.studio.exampleapp.utils.Event
import com.clover.studio.exampleapp.utils.Tools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepositoryImpl,
    private val sharedPrefsRepo: SharedPreferencesRepository
) : ViewModel() {

    val usersListener = MutableLiveData<Event<MainStates>>()

    fun getContacts() = viewModelScope.launch {
        try {
            val users = repository.getUsers().data?.list
            usersListener.postValue(Event(UsersFetched(users!!)))
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
        emitSource(repository.getUserLocal())
    }
}

sealed class MainStates
data class UsersFetched(val userData: List<User>) : MainStates()
object UsersError : MainStates()