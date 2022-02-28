package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.networking.ContactResponse
import com.clover.studio.exampleapp.utils.Const
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HeaderMap

interface RetrofitService {
    @GET(Const.Networking.API_CONTACTS)
    suspend fun getUsers(
        @HeaderMap headers: Map<String, String?>
    ): ContactResponse
}