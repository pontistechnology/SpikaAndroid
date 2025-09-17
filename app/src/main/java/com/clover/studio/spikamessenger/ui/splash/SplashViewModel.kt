package com.clover.studio.spikamessenger.ui.splash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.spikamessenger.BaseViewModel
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sharedPrefsRepo: SharedPreferencesRepository
) : BaseViewModel() {

    var splashTokenListener = MutableLiveData<Event<SplashStates>>()

    fun checkToken() = viewModelScope.launch {
        if (!sharedPrefsRepo.readToken().isNullOrEmpty()
            && !sharedPrefsRepo.isAccountCreated()
        ) {
            splashTokenListener.postValue(Event(SplashStates.NAVIGATE_ACCOUNT_CREATION))
        } else if (sharedPrefsRepo.readToken()
                .isNullOrEmpty() || !sharedPrefsRepo.readRegistered()
        ) {
            splashTokenListener.postValue(Event(SplashStates.NAVIGATE_ONBOARDING))
        } else {
            splashTokenListener.postValue(Event(SplashStates.NAVIGATE_MAIN))
        }
    }
}

enum class SplashStates { NAVIGATE_ONBOARDING, NAVIGATE_MAIN, NAVIGATE_ACCOUNT_CREATION, NAVIGATE_REGISTER_NUMBER }
