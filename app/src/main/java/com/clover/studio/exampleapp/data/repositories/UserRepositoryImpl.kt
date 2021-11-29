package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.networking.ContactResponse
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao
) : UserRepository {
    fun getUserLocal() = userDao.getUsers()

    override suspend fun getUsers(token: String, page: Int): ContactResponse =
        retrofitService.getUsers(token, page)
}

interface UserRepository {
    suspend fun getUsers(token: String, page: Int): ContactResponse
}