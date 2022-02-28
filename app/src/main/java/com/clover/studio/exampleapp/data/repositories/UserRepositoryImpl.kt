package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.models.networking.ContactResponse
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao,
    private val sharedPrefs: SharedPreferencesRepository
) : UserRepository {
    fun getUserLocal() = userDao.getUsers()

    override suspend fun getUsers(): ContactResponse =
        retrofitService.getUsers(getHeaderMap(sharedPrefs.readToken()!!))

    override fun getUserByID(id: Int) =
        userDao.getUserById(id)
}

interface UserRepository {
    suspend fun getUsers(): ContactResponse
    fun getUserByID(id: Int): LiveData<User>
}