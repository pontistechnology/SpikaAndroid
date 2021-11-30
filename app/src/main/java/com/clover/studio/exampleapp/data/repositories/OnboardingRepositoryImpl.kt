package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.data.services.OnboardingService
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val retrofitService: OnboardingService,
    private val userDao: UserDao,
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
    ): AuthResponse {
        val data = retrofitService.verifyUserCode(code, deviceId)

        userDao.insert(data.user)

        return data
    }

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