package com.clover.studio.spikamessenger.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.clover.studio.spikamessenger.BaseViewModel
import com.clover.studio.spikamessenger.data.models.entity.PhoneUser
import com.clover.studio.spikamessenger.data.models.networking.responses.AuthResponse
import com.clover.studio.spikamessenger.data.repositories.OnboardingRepositoryImpl
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.utils.Event
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.helpers.Resource
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

    var codeVerificationListener = MutableLiveData<Event<Resource<AuthResponse?>>>()
    var registrationListener = MutableLiveData<Event<Resource<AuthResponse?>>>()
    var accountCreationListener = MutableLiveData<Event<Resource<AuthResponse?>>>()
    var userUpdateListener = MutableLiveData<Event<Resource<AuthResponse?>>>()
    var userPhoneNumberListener = MutableLiveData<String>()

    fun sendNewUserData(
        jsonObject: JsonObject
    ) = viewModelScope.launch {
        resolveResponseStatus(registrationListener, onboardingRepository.sendUserData(jsonObject))
    }

    fun sendCodeVerification(jsonObject: JsonObject) = CoroutineScope(Dispatchers.IO).launch {
        resolveResponseStatus(codeVerificationListener, Resource(Resource.Status.LOADING, null, ""))

        val response = onboardingRepository.verifyUserCode(jsonObject)

        if (Resource.Status.ERROR != response.status && Resource.Status.TOKEN_EXPIRED != response.status) {
            response.responseData?.data?.device?.token?.let { sharedPrefs.writeToken(it) }
            if (sharedPrefs.isNewUser()) {
                resolveResponseStatus(
                    codeVerificationListener,
                    Resource(Resource.Status.NEW_USER, null, "")
                )
            } else {
                sharedPrefs.accountCreated(true)
                resolveResponseStatus(
                    codeVerificationListener,
                    Resource(Resource.Status.SUCCESS, null, "")
                )
            }
        } else {
            resolveResponseStatus(
                codeVerificationListener,
                Resource(Resource.Status.ERROR, null, "")
            )
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
            resolveResponseStatus(
                accountCreationListener,
                Resource(Resource.Status.ERROR, null, "")
            )
            return@launch
        }
        Timber.d("$contacts")
        resolveResponseStatus(
            accountCreationListener,
            onboardingRepository.sendUserContacts(contacts!!)
        )
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
        resolveResponseStatus(userUpdateListener, onboardingRepository.updateUser(jsonObject))
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

    fun writePhoneAndCountry(phoneNumber: String, countryCode: String) =
        viewModelScope.launch {
            sharedPrefs.writeUserPhoneDetails(phoneNumber, countryCode)
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

    fun writeDeviceId(deviceId: String) = viewModelScope.launch {
        sharedPrefs.writeDeviceId(deviceId)
    }

    fun readDeviceId(): String? {
        var deviceId: String?
        runBlocking {
            deviceId = sharedPrefs.readDeviceId()
        }
        return deviceId
    }
}