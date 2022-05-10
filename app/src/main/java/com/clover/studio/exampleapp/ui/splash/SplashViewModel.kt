package com.clover.studio.exampleapp.ui.splash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sharedPrefsRepo: SharedPreferencesRepository
) : ViewModel() {

    var splashTokenListener = MutableLiveData<Event<SplashStates>>()

    fun checkToken() = viewModelScope.launch {
        if (sharedPrefsRepo.readToken().isNullOrEmpty()
            && !sharedPrefsRepo.isAccountCreated()
        ) {
            splashTokenListener.postValue(Event(SplashStates.NAVIGATE_ONBOARDING))
        } else if (!sharedPrefsRepo.readToken().isNullOrEmpty()
            && !sharedPrefsRepo.isAccountCreated()
        ) {
            splashTokenListener.postValue(Event(SplashStates.NAVIGATE_ACCOUNT_CREATION))
        } else {
            splashTokenListener.postValue(Event(SplashStates.NAVIGATE_MAIN))
        }
    }
}

enum class SplashStates { NAVIGATE_ONBOARDING, NAVIGATE_MAIN, NAVIGATE_ACCOUNT_CREATION }