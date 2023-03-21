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
import com.clover.studio.exampleapp.utils.helpers.Resource
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

    var codeVerificationListener = MutableLiveData<Event<Resource.Status>>()
    var registrationListener = MutableLiveData<Event<Resource<AuthResponse>>>()
    var accountCreationListener = MutableLiveData<Event<Resource.Status>>()
    var userUpdateListener = MutableLiveData<Event<Resource<AuthResponse>>>()
    var userPhoneNumberListener = MutableLiveData<String>()

    fun sendNewUserData(
        jsonObject: JsonObject
    ) = viewModelScope.launch {
        registrationListener.postValue(Event(onboardingRepository.sendUserData(jsonObject)))
    }

    fun sendCodeVerification(jsonObject: JsonObject) = CoroutineScope(Dispatchers.IO).launch {
        codeVerificationListener.postValue(Event(Resource.Status.LOADING))

        val response = onboardingRepository.verifyUserCode(jsonObject)
        codeVerificationListener.postValue(Event(response.status))

        Timber.d("Token ${response.responseData!!.data.device.token}")
        response.responseData.data.device.token?.let { sharedPrefs.writeToken(it) }

        if (sharedPrefs.isNewUser())
            codeVerificationListener.postValue(Event(Resource.Status.NEW_USER))
        else {
            sharedPrefs.accountCreated(true)
            codeVerificationListener.postValue(Event(Resource.Status.SUCCESS))
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
            accountCreationListener.postValue(Event(Resource.Status.ERROR))
            return@launch
        }

        Timber.d("$contacts")

        val response = onboardingRepository.sendUserContacts(contacts!!)
        accountCreationListener.postValue(Event(response.status))

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

    fun updateUserData(jsonObject: JsonObject) = CoroutineScope(Dispatchers.IO).launch {
        userUpdateListener.postValue(Event(onboardingRepository.updateUser(jsonObject)))
    }

    fun writePhoneUsers(phoneUsers: List<PhoneUser>) = CoroutineScope(Dispatchers.IO).launch {
        onboardingRepository.writePhoneUsers(phoneUsers)
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