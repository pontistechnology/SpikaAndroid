package com.clover.studio.exampleapp.data.repositories.data_sources

import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.clover.studio.exampleapp.utils.helpers.BaseDataSource
import com.google.gson.JsonObject
import javax.inject.Inject

class MainRemoteDataSource @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao,
    private val sharedPrefs: SharedPreferencesRepository
) : BaseDataSource() {
    suspend fun getRoomById(userId: Int) = getResult {
        retrofitService.getRoomById(getHeaderMap(sharedPrefs.readToken()), userId)
    }

    suspend fun verifyFile(jsonObject: JsonObject) = getResult {
        retrofitService.verifyFile(getHeaderMap(sharedPrefs.readToken()), jsonObject)
    }

    suspend fun updatePushToken(jsonObject: JsonObject) = getResult {
        retrofitService.updatePushToken(getHeaderMap(sharedPrefs.readToken()), jsonObject)
    }
}