package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.User
import retrofit2.Response
import retrofit2.http.GET

interface RetrofitService {
    // implement calls to API
    @GET
    suspend fun getUsers(): Response<List<User>>
}