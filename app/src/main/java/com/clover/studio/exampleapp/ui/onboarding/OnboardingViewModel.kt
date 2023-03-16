package com.clover.studio.exampleapp.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.exampleapp.BaseViewModel
import com.clover.studio.exampleapp.data.models.entity.PhoneUser
import com.clover.studio.exampleapp.data.models.networking.responses.AuthResponse
import com.clover.studio.exampleapp.data.repositories.OnboardingRepositoryImpl
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.utils.Event
import com.clover.studio.exampleapp.utils.Tools
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepositoryImpl,
    private val sharedPrefs: SharedPreferencesRepository
) : BaseViewModel() {

    var codeVerificationListener = MutableLiveData<Event<OnboardingStates>>()
    var registrationListener = MutableLiveData<Event<OnboardingStates>>()
    var accountCreationListener = MutableLiveData<Event<OnboardingStates>>()
    var userUpdateListener = MutableLiveData<Event<OnboardingStates>>()
    var userPhoneNumberListener = MutableLiveData<String>()

    fun sendNewUserData(
        jsonObject: JsonObject
    ) = viewModelScope.launch {
        try {
            registrationListener.postValue(Event(OnboardingStates.REGISTERING_IN_PROGRESS))
            onboardingRepository.sendUserData(jsonObject)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            registrationListener.postValue(Event(OnboardingStates.REGISTERING_ERROR))
            return@launch
        }

        registrationListener.postValue(Event(OnboardingStates.REGISTERING_SUCCESS))
    }

    fun sendCodeVerification(jsonObject: JsonObject) = CoroutineScope(Dispatchers.IO).launch {
        codeVerificationListener.postValue(Event(OnboardingStates.VERIFYING))
        val authResponse: AuthResponse
        try {
            authResponse = onboardingRepository.verifyUserCode(jsonObject)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            codeVerificationListener.postValue(Event(OnboardingStates.CODE_ERROR))
            return@launch
        }

        Timber.d("Token ${authResponse.data.device.token}")
        authResponse.data.device.token?.let { sharedPrefs.writeToken(it) }

        if (sharedPrefs.isNewUser())
            codeVerificationListener.postValue(Event((OnboardingStates.CODE_VERIFIED_NEW_USER)))
        else {
            sharedPrefs.accountCreated(true)
            codeVerificationListener.postValue(Event(OnboardingStates.CODE_VERIFIED))
        }
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

    fun updateUserData(jsonObject: JsonObject) = viewModelScope.launch {
        try {
            onboardingRepository.updateUser(jsonObject)
            sharedPrefs.accountCreated(true)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            userUpdateListener.postValue(Event(OnboardingStates.USER_UPDATE_ERROR))
            return@launch
        }

        userUpdateListener.postValue(Event(OnboardingStates.USER_UPDATED))
    }

    fun writePhoneUsers(phoneUsers: List<PhoneUser>) = viewModelScope.launch {
        try {
            onboardingRepository.writePhoneUsers(phoneUsers)
        } catch (ex: Exception) {
            Tools.checkError(ex)
            return@launch
        }
    }

    fun writeFirstAppStart() = viewModelScope.launch {
        sharedPrefs.writeFirstAppStart(true)
    }

    fun isAppStarted(): Boolean {
        var flag: Boolean
        runBlocking {
            flag = sharedPrefs.isFirstAppStart()
        }
        return flag
    }

    fun writePhoneAndDeviceId(phoneNumber: String, deviceId: String, countryCode: String) =
        viewModelScope.launch {
            sharedPrefs.writeUserPhoneDetails(phoneNumber, deviceId, countryCode)
        }

    fun readPhoneNumber(): String {
        var number: String
        runBlocking {
            number = sharedPrefs.readPhoneNumber().toString()
        }
        return number
    }

    fun readCountryCode(): String {
        var countryCode: String
        runBlocking {
            countryCode = sharedPrefs.readCountryCode().toString()
        }
        return countryCode
    }

    fun registerFlag(flag: Boolean) = viewModelScope.launch {
        sharedPrefs.writeRegistered(flag)
    }

    fun readDeviceId(): String {
        var deviceId: String
        runBlocking {
            deviceId = sharedPrefs.readDeviceId().toString()
        }
        return deviceId
    }
}

enum class OnboardingStates { VERIFYING, CODE_VERIFIED, CODE_VERIFIED_NEW_USER, CODE_ERROR, REGISTERING_SUCCESS, REGISTERING_IN_PROGRESS, REGISTERING_ERROR, CONTACTS_SENT, CONTACTS_ERROR, USER_UPDATED, USER_UPDATE_ERROR }