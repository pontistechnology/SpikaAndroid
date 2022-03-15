package com.clover.studio.exampleapp.data.repositories

import androidx.lifecycle.LiveData
import com.clover.studio.exampleapp.data.daos.UserDao
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.services.RetrofitService
import com.clover.studio.exampleapp.utils.Tools.getHeaderMap
import com.google.gson.JsonObject
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val userDao: UserDao,
    private val sharedPrefs: SharedPreferencesRepository
) : UserRepository {
    override suspend fun getUsers() {
        val userData = retrofitService.getUsers(getHeaderMap(sharedPrefs.readToken()!!))

        if (userData.data?.list != null) {
            for (user in userData.data.list) {
                userDao.insert(user)
            }
        }
    }

    override fun getUserByID(id: Int) =
        userDao.getUserById(id)

    override fun getUserLiveData(): LiveData<List<User>> =
        userDao.getUsers()

    override suspend fun getRoomById(userId: Int) =
        retrofitService.getRoomById(getHeaderMap(sharedPrefs.readToken()), userId)

    override suspend fun createNewRoom(jsonObject: JsonObject) =
        retrofitService.createNewRoom(getHeaderMap(sharedPrefs.readToken()), jsonObject)

}

interface UserRepository {
    suspend fun getUsers()
    fun getUserByID(id: Int): LiveData<User>
    fun getUserLiveData(): LiveData<List<User>>
    suspend fun getRoomById(userId: Int)
    suspend fun createNewRoom(jsonObject: JsonObject)
}