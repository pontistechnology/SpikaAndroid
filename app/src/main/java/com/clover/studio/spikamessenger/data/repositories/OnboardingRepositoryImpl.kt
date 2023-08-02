package com.clover.studio.spikamessenger.data.repositories

import com.clover.studio.spikamessenger.data.daos.PhoneUserDao
import com.clover.studio.spikamessenger.data.daos.UserDao
import com.clover.studio.spikamessenger.data.models.entity.PhoneUser
import com.clover.studio.spikamessenger.data.models.networking.responses.AuthResponse
import com.clover.studio.spikamessenger.data.repositories.data_sources.OnboardingRemoteDataSource
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.clover.studio.spikamessenger.utils.helpers.RestOperations.performRestOperation
import com.clover.studio.spikamessenger.utils.helpers.RestOperations.queryDatabaseCoreData
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

        response.responseData?.data?.isNewUser?.let { sharedPrefs.setNewUser(it) }

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

        response.responseData?.data?.user?.id?.let { sharedPrefs.writeUserId(it) }

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

        if (Resource.Status.SUCCESS == response.status) {
            sharedPrefs.accountCreated(true)
            response.responseData?.data?.user?.id?.let { sharedPrefs.writeUserId(it) }
        }

        return response
    }
}

interface OnboardingRepository : BaseRepository {
    suspend fun sendUserData(jsonObject: JsonObject): Resource<AuthResponse>
    suspend fun verifyUserCode(jsonObject: JsonObject): Resource<AuthResponse>
    suspend fun sendUserContacts(contacts: List<String>): Resource<AuthResponse>
    suspend fun writePhoneUsers(phoneUsers: List<PhoneUser>)
    suspend fun updateUser(jsonObject: JsonObject): Resource<AuthResponse>
}