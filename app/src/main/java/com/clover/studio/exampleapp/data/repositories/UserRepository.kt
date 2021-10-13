package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao
) {
    fun getUserLocal() = userDao.getUsers()
}