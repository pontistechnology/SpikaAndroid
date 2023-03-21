package com.clover.studio.exampleapp.data.repositories.data_sources

import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.data.services.OnboardingService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.clover.studio.exampleapp.utils.helpers.BaseDataSource
import com.google.gson.JsonObject
import javax.inject.Inject

class OnboardingRemoteDataSource @Inject constructor(
    private val retrofitService: OnboardingService,
    private val sharedPrefs: SharedPreferencesRepository
) : BaseDataSource() {

    suspend fun sendUserData(jsonObject: JsonObject) = getResult {
        retrofitService.sendUserData(getHeaderMap(sharedPrefs.readToken()), jsonObject)
    }

    suspend fun verifyUserCode(jsonObject: JsonObject) = getResult {
        retrofitService.verifyUserCode(getHeaderMap(sharedPrefs.readToken()), jsonObject)
    }

    suspend fun sendContacts(contacts: List<String>) = getResult {
        retrofitService.sendContacts(getHeaderMap(sharedPrefs.readToken()), contacts)
    }

    suspend fun updateUser(jsonObject: JsonObject) = getResult {
        retrofitService.updateUser(getHeaderMap(sharedPrefs.readToken()), jsonObject)
    }
}
