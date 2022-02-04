package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.networking.ContactResponse
import com.clover.studio.exampleapp.utils.Const
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.HeaderMap
import retrofit2.http.Query

interface RetrofitService {
    @GET(Const.Networking.API_CONTACTS)
    suspend fun getUsers(
        @Header("accesstoken") token: String,
        @Query("page") page: Int
    ): ContactResponse

    @GET(Const.Networking.API_GET_MESSAGES)
    suspend fun getMessages(
        @HeaderMap headers: Map<String, String?>,
        @Query("timestamp") timestamp: Int
    ): List<Message>
}