package com.clover.studio.spikamessenger.data.services

import com.clover.studio.spikamessenger.data.models.networking.responses.*
import com.clover.studio.spikamessenger.utils.Const
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface SSEService {
    @GET(Const.Networking.API_SYNC_MESSAGES)
    suspend fun syncMessages(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.LAST_UPDATE) lastUpdate: Long,
        @Query(Const.Networking.PAGE) page: Int
    ): Response<MessageResponse>

    @GET(Const.Networking.API_SYNC_MESSAGE_RECORDS)
    suspend fun syncMessageRecords(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.LAST_UPDATE) lastUpdate: Long,
        @Query(Const.Networking.PAGE) page: Int
    ): Response<MessageRecordsResponse>

    @GET(Const.Networking.API_SYNC_USERS)
    suspend fun syncUsers(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.LAST_UPDATE) lastUpdate: Long,
        @Query(Const.Networking.PAGE) page: Int
    ): Response<ContactResponse>

    @GET(Const.Networking.API_SYNC_ROOMS)
    suspend fun syncRooms(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.LAST_UPDATE) lastUpdate: Long,
        @Query(Const.Networking.PAGE) page: Int
    ): Response<RoomResponse>

    @POST(Const.Networking.API_MESSAGE_DELIVERED)
    suspend fun sendMessageDelivered(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): Response<MessageRecordsResponse>

    @GET(Const.Networking.API_UNREAD_COUNT)
    suspend fun getUnreadCount(
        @HeaderMap headers: Map<String, String?>
    ): Response<UnreadCountResponse>

    @FormUrlEncoded
    @POST(Const.Networking.API_CONTACTS)
    suspend fun syncContacts(
        @HeaderMap headers: Map<String, String?>,
        @Field(Const.Networking.CONTACTS) contacts: List<String>,
        @Field(Const.Networking.IS_LAST_PAGE) isLastPage: Boolean
    ): Response<ContactsSyncResponse>

    @GET(Const.Networking.API_SETTINGS)
    suspend fun getAppMode(
        @HeaderMap headers: Map<String, String?>
    ): Response<SettingsResponse>
}
