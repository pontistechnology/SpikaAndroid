package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val sharedPrefs: SharedPreferencesRepository
) : OnboardingRepository {
    override suspend fun sendUserData(
        phoneNumber: String,
        phoneNumberHashed: String,
        countryCode: String,
        deviceId: String
    ) {
        retrofitService.sendUserData(phoneNumber, phoneNumberHashed, countryCode, deviceId)
    }

    override suspend fun verifyUserCode(
        code: String,
        deviceId: String
    ): AuthResponse = retrofitService.verifyUserCode(code, deviceId)

    override suspend fun sendUserContacts(
        token: String,
        contacts: List<String>
    ): AuthResponse = retrofitService.sendContacts(token, contacts)

}

interface OnboardingRepository {
    suspend fun sendUserData(
        phoneNumber: String,
        phoneNumberHashed: String,
        countryCode: String,
        deviceId: String
    )

    suspend fun verifyUserCode(
        code: String,
        deviceId: String
    ): AuthResponse

    suspend fun sendUserContacts(
        token: String,
        contacts: List<String>
    ): AuthResponse
}