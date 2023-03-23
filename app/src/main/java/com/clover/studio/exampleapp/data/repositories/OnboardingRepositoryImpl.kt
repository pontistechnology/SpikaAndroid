package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.PhoneUserDao
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.entity.PhoneUser
import com.clover.studio.exampleapp.data.models.networking.responses.AuthResponse
import com.clover.studio.exampleapp.data.repositories.data_sources.OnboardingRemoteDataSource
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.clover.studio.exampleapp.utils.helpers.RestOperations.performRestOperation
import com.clover.studio.exampleapp.utils.helpers.RestOperations.queryDatabaseCoreData
import com.google.gson.JsonObject
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val onboardingRemoteDataSource: OnboardingRemoteDataSource,
    private val userDao: UserDao,
    private val phoneUserDao: PhoneUserDao,
    private val sharedPrefs: SharedPreferencesRepository
) : OnboardingRepository {
    override suspend fun sendUserData(
        jsonObject: JsonObject
    ): Resource<AuthResponse> {
        val response = performRestOperation(
            networkCall = { onboardingRemoteDataSource.sendUserData(jsonObject) },
        )

        sharedPrefs.setNewUser(response.responseData?.data!!.isNewUser)

        return response
    }

    override suspend fun verifyUserCode(
        jsonObject: JsonObject
    ): Resource<AuthResponse> {
        val response =
            performRestOperation(
                networkCall = { onboardingRemoteDataSource.verifyUserCode(jsonObject) },
                saveCallResult = { userDao.upsert(it.data.user) }
            )

        sharedPrefs.writeUserId(response.responseData!!.data.user.id)

        return response
    }

    override suspend fun sendUserContacts(
        contacts: List<String>
    ) = performRestOperation(
        networkCall = { onboardingRemoteDataSource.sendContacts(contacts) },
    )

    override suspend fun writePhoneUsers(phoneUsers: List<PhoneUser>) {
        queryDatabaseCoreData(
            databaseQuery = { phoneUserDao.upsert(phoneUsers) }
        )
    }

    override suspend fun updateUser(
        jsonObject: JsonObject
    ): Resource<AuthResponse> {
        val response =
            performRestOperation(
                networkCall = { onboardingRemoteDataSource.updateUser(jsonObject) },
                saveCallResult = { userDao.upsert(it.data.user) }
            )

        sharedPrefs.writeUserId(response.responseData?.data?.user!!.id)

        return response
    }
}

interface OnboardingRepository {
    suspend fun sendUserData(jsonObject: JsonObject): Resource<AuthResponse>
    suspend fun verifyUserCode(jsonObject: JsonObject): Resource<AuthResponse>
    suspend fun sendUserContacts(contacts: List<String>): Resource<AuthResponse>
    suspend fun writePhoneUsers(phoneUsers: List<PhoneUser>)
    suspend fun updateUser(jsonObject: JsonObject): Resource<AuthResponse>
}