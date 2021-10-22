package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao
) : UserRepository {
    fun getUserLocal() = userDao.getUsers()

    override suspend fun getUser(): User {
        TODO("Not yet implemented")
    }
}

interface UserRepository {
    suspend fun getUser(): User
}