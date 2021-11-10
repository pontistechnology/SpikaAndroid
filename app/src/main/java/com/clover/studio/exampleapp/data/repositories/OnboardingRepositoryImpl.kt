package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService
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
    ) {
        retrofitService.verifyUserCode(code, deviceId)
    }

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
    )
}