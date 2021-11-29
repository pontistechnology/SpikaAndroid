package com.clover.studio.exampleapp.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.data.repositories.UserRepositoryImpl
import com.clover.studio.exampleapp.utils.Tools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UserRepositoryImpl,
    private val sharedPrefsRepo: SharedPreferencesRepository
) : ViewModel() {

    val usersListener = MutableLiveData<MainStates>()

    fun getContacts(page: Int) = viewModelScope.launch {
        try {
            val users = repository.getUsers(sharedPrefsRepo.readToken()!!, page).data?.list
            usersListener.value = UsersFetched(users!!)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            usersListener.postValue(UsersError)
            return@launch
        }
    }

    fun getLocalUsers() = liveData {
        emitSource(repository.getUserLocal())
    }
}

sealed class MainStates
data class UsersFetched(val userData: List<User>) : MainStates()
object UsersError : MainStates()