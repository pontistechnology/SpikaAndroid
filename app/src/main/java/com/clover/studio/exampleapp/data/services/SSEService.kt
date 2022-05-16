package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.networking.ContactResponse
import com.clover.studio.exampleapp.data.models.networking.MessageRecordsResponse
import com.clover.studio.exampleapp.data.models.networking.MessageResponse
import com.clover.studio.exampleapp.utils.Const
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Path

interface SSEService {
    @GET(Const.Networking.API_SYNC_MESSAGES)
    suspend fun syncMessages(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.TIMESTAMP) timestamp: Long
    ): MessageResponse

    @GET(Const.Networking.API_SYNC_MESSAGE_RECORDS)
    suspend fun syncMessageRecords(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.LAST_UPDATE) lastUpdate: Long
    ): MessageRecordsResponse

    @GET(Const.Networking.API_SYNC_USERS)
    suspend fun syncUsers(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.TIMESTAMP) timestamp: Long
    ): ContactResponse
}