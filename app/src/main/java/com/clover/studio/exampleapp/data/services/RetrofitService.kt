package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.utils.Const
import retrofit2.Response
import retrofit2.http.*

interface RetrofitService {
    // implement calls to API
    @GET
    suspend fun getUsers(): Response<List<User>>
}