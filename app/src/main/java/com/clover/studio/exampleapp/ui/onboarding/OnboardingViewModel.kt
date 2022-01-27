package com.clover.studio.exampleapp.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.data.repositories.OnboardingRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.Event
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepositoryImpl,
    private val sharedPrefs: SharedPreferencesRepository
) : ViewModel() {

    var codeVerificationListener = MutableLiveData<Event<OnboardingStates>>()
    var registrationListener = MutableLiveData<Event<OnboardingStates>>()
    var accountCreationListener = MutableLiveData<Event<OnboardingStates>>()
    var userUpdateListener = MutableLiveData<Event<OnboardingStates>>()

    fun sendNewUserData(
        phoneNumber: String,
        phoneNumberHashed: String,
        countryCode: String,
        deviceId: String
    ) = viewModelScope.launch {
        try {
            onboardingRepository.sendUserData(phoneNumber, phoneNumberHashed, countryCode, deviceId)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            registrationListener.postValue(Event(OnboardingStates.REGISTERING_ERROR))
            return@launch
        }

        registrationListener.postValue(Event(OnboardingStates.REGISTERING_SUCCESS))
    }

    fun sendCodeVerification(
        code: String,
        deviceId: String
    ) = viewModelScope.launch {
        codeVerificationListener.postValue(Event(OnboardingStates.VERIFYING))
        val authResponse: AuthResponse
        try {
            authResponse = onboardingRepository.verifyUserCode(code, deviceId)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            codeVerificationListener.postValue(Event(OnboardingStates.CODE_ERROR))
            return@launch
        }

        Timber.d("Token ${authResponse.data.device.token}")
        authResponse.data.device.token?.let { sharedPrefs.writeToken(it) }
        codeVerificationListener.postValue(Event(OnboardingStates.CODE_VERIFIED))
    }

    fun readToken() {
        viewModelScope.launch {
            Timber.d("Token ${sharedPrefs.readToken()}")
        }
    }

    fun sendContacts() = viewModelScope.launch {

        val contacts: List<String>?
        try {
            contacts = sharedPrefs.readContacts()
        } catch (ex: Exception) {
            Tools.checkError(ex)
            accountCreationListener.postValue(Event(OnboardingStates.CONTACTS_ERROR))
            return@launch
        }

        Timber.d("$contacts")

        try {
            contacts?.let {
                onboardingRepository.sendUserContacts(it)
            }
        } catch (ex: Exception) {
            Tools.checkError(ex)
            accountCreationListener.postValue(Event(OnboardingStates.CONTACTS_ERROR))
            return@launch
        }

        accountCreationListener.postValue(Event(OnboardingStates.CONTACTS_SENT))
    }

    fun writeContactsToSharedPref(contacts: List<String>) {
        viewModelScope.launch {
            sharedPrefs.writeContacts(contacts)
        }
    }

    fun areUsersFetched(): Boolean {
        var contactsFilled = false

        viewModelScope.launch {
            if (!sharedPrefs.readContacts().isNullOrEmpty()) {
                contactsFilled = true
            }
        }

        return contactsFilled
    }

    fun updateUserData(userMap: Map<String, String>) = viewModelScope.launch {
        try {
            onboardingRepository.updateUser(userMap)
            sharedPrefs.accountCreated(true)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            userUpdateListener.postValue(Event(OnboardingStates.USER_UPDATE_ERROR))
            return@launch
        }

        userUpdateListener.postValue(Event(OnboardingStates.USER_UPDATED))
    }
}

enum class OnboardingStates { VERIFYING, CODE_VERIFIED, CODE_ERROR, REGISTERING_SUCCESS, REGISTERING_ERROR, CONTACTS_SENT, CONTACTS_ERROR, USER_UPDATED, USER_UPDATE_ERROR }