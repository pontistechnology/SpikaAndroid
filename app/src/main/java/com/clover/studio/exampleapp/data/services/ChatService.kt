package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.networking.MessageRecordsResponse
import com.clover.studio.exampleapp.data.models.networking.MessageResponse
import com.clover.studio.exampleapp.utils.Const
import com.google.gson.JsonObject
import retrofit2.http.*

interface ChatService {
    @POST(Const.Networking.API_POST_MESSAGE)
    suspend fun sendMessage(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): Message

    @GET(Const.Networking.API_GET_MESSAGES)
    suspend fun getMessages(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ROOM_ID) roomId: String
    ): MessageResponse

    @GET(Const.Networking.API_GET_MESSAGES_TIMESTAMP)
    suspend fun getMessagesTimestamp(
        @HeaderMap headers: Map<String, String?>,
        @Query(Const.Networking.TIMESTAMP) timestamp: Int
    ): MessageResponse

    @POST(Const.Networking.API_MESSAGE_DELIVERED)
    suspend fun sendMessageDelivered(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    )

//    @GET(Const.Networking.API_GET_MESSAGE_RECORDS)
//    suspend fun getMessageRecords(
//        @HeaderMap headers: Map<String, String?>,
//        @Path(Const.Networking.MESSAGE_ID) messageId: String
//    ): MessageRecordsResponse

    @GET(Const.Networking.API_SSE_STREAM)
    @Streaming
    suspend fun getPushNotification(
        @Path(Const.Networking.DEVICE_ID) deviceId: Int,
        @Query(Const.Headers.ACCESS_TOKEN) token: String
    ): Message
}