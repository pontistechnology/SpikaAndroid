package com.clover.studio.exampleapp.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.data.repositories.OnboardingRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.Tools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepositoryImpl,
    private val sharedPrefs: SharedPreferencesRepository
) : ViewModel() {

    var codeVerificationListener = MutableLiveData<OnboardingStates>()
    var registrationListener = MutableLiveData<OnboardingStates>()
    var accountCreationListener = MutableLiveData<OnboardingStates>()

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
            registrationListener.postValue(OnboardingStates.REGISTERING_ERROR)
            return@launch
        }

        registrationListener.postValue(OnboardingStates.REGISTERING_SUCCESS)
    }

    fun sendCodeVerification(
        code: String,
        deviceId: String
    ) = viewModelScope.launch {
        codeVerificationListener.postValue(OnboardingStates.VERIFYING)
        val authResponse: AuthResponse
        try {
            authResponse = onboardingRepository.verifyUserCode(code, deviceId)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            codeVerificationListener.postValue(OnboardingStates.CODE_ERROR)
            return@launch
        }

        Timber.d("Token ${authResponse.device.token}")
        authResponse.device.token?.let { sharedPrefs.writeToken(it) }
        codeVerificationListener.postValue(OnboardingStates.CODE_VERIFIED)
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
            accountCreationListener.postValue(OnboardingStates.CONTACTS_ERROR)
            return@launch
        }

        Timber.d("$contacts")

        try {
            contacts?.let { onboardingRepository.sendUserContacts(sharedPrefs.readToken()!!, it) }
        } catch (ex: Exception) {
            Tools.checkError(ex)
            accountCreationListener.postValue(OnboardingStates.CONTACTS_ERROR)
            return@launch
        }

        accountCreationListener.postValue(OnboardingStates.CONTACTS_SENT)
    }

    fun writeContactsToSharedPref(contacts: List<String>) {
        viewModelScope.launch {
            sharedPrefs.writeContacts(contacts)
        }
    }
}

enum class OnboardingStates { VERIFYING, CODE_VERIFIED, CODE_ERROR, REGISTERING_SUCCESS, REGISTERING_ERROR, CONTACTS_SENT, CONTACTS_ERROR }