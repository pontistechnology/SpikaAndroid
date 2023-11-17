package com.clover.studio.spikamessenger

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepositoryImpl
import com.clover.studio.spikamessenger.utils.Event
import com.clover.studio.spikamessenger.utils.helpers.Resource
import kotlinx.coroutines.launch
import timber.log.Timber

open class BaseViewModel : ViewModel() {

    var sharedPrefs: SharedPreferencesRepository =
        SharedPreferencesRepositoryImpl(MainApplication.appContext)
    val tokenExpiredListener = MutableLiveData<Event<Boolean>>()

    fun setTokenExpiredTrue() {
        tokenExpiredListener.postValue(Event(true))
    }

    fun setTokenExpiredFalse() {
        tokenExpiredListener.postValue(Event(false))
    }

    fun removeToken() = viewModelScope.launch {
        sharedPrefs.writeToken("")
    }

    fun <R> resolveResponseStatus(
        mutableLiveData: MutableLiveData<Event<Resource<R?>>>?,
        resource: Resource<R?>
    ) {
        when (resource.status) {
            Resource.Status.SUCCESS, Resource.Status.ERROR -> mutableLiveData?.postValue(
                Event(
                    resource
                )
            )

            Resource.Status.TOKEN_EXPIRED -> tokenExpiredListener.postValue(Event(true))
            Resource.Status.LOADING -> {
                mutableLiveData?.postValue(Event(resource))
            }

            Resource.Status.NEW_USER -> {
                mutableLiveData?.postValue(Event(resource))
            }

            else -> Timber.d("Error in BaseViewModel while resolving response status")
        }
    }

    fun getUserTheme(): String? {
        var theme: String? = null
        viewModelScope.launch {
            theme = sharedPrefs.readUserTheme()
        }
        return theme
    }
}
