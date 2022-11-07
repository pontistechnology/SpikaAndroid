package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.networking.ContactResponse
import com.clover.studio.exampleapp.data.models.networking.MessageRecordsResponse
import com.clover.studio.exampleapp.data.models.networking.MessageResponse
import com.clover.studio.exampleapp.data.models.networking.RoomResponse
import com.clover.studio.exampleapp.utils.Const
import com.google.gson.JsonObject
import retrofit2.http.*

interface SSEService {
    @GET(Const.Networking.API_SYNC_MESSAGES)
    suspend fun syncMessages(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.LAST_UPDATE) lastUpdate: Long
    ): MessageResponse

    @GET(Const.Networking.API_SYNC_MESSAGE_RECORDS)
    suspend fun syncMessageRecords(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.LAST_UPDATE) lastUpdate: Long
    ): MessageRecordsResponse

    @GET(Const.Networking.API_SYNC_USERS)
    suspend fun syncUsers(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.LAST_UPDATE) lastUpdate: Long
    ): ContactResponse

    @GET(Const.Networking.API_SYNC_ROOMS)
    suspend fun syncRooms(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.LAST_UPDATE) lastUpdate: Long
    ): RoomResponse

    @POST(Const.Networking.API_MESSAGE_DELIVERED)
    suspend fun sendMessageDelivered(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): MessageRecordsResponse
}