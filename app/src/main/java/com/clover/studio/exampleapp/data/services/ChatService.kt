package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.networking.MessageResponse
import com.clover.studio.exampleapp.data.models.networking.MuteResponse
import com.clover.studio.exampleapp.data.models.networking.RoomResponse
import com.clover.studio.exampleapp.data.models.networking.SettingsResponse
import com.clover.studio.exampleapp.utils.Const
import com.google.gson.JsonObject
import retrofit2.http.*

interface ChatService {
    @POST(Const.Networking.API_POST_MESSAGE)
    suspend fun sendMessage(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): MessageResponse

    @POST(Const.Networking.API_MESSAGES_SEEN)
    suspend fun sendMessagesSeen(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ROOM_ID) roomId: Int
    )

    @PUT(Const.Networking.API_UPDATE_ROOM)
    suspend fun updateRoom(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject,
        @Path(Const.Networking.ROOM_ID) roomId: Int
    ): RoomResponse

    @POST(Const.Networking.API_MUTE_ROOM)
    suspend fun muteRoom(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ROOM_ID) roomId: Int
    ): MuteResponse

    @POST(Const.Networking.API_UNMUTE_ROOM)
    suspend fun unmuteRoom(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ROOM_ID) roomId: Int
    ): MuteResponse

    @GET(Const.Networking.API_GET_SETTINGS)
    suspend fun getSettings(
        @HeaderMap headers: Map<String, String?>
    ): SettingsResponse

    // Post reaction:
    @POST(Const.Networking.API_POST_REACTION)
    suspend fun postReaction(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    )

    // Delete reaction:
    @DELETE(Const.Networking.API_DELETE_REACTION)
    suspend fun deleteReaction(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ID) id: Int
    )

    @DELETE(Const.Networking.API_UPDATE_ROOM)
    suspend fun deleteRoom(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ROOM_ID) roomId: Int
    ): RoomResponse

    @DELETE(Const.Networking.API_UPDATE_MESSAGE)
    suspend fun deleteMessage(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ID) messageId: Int,
        @Query(Const.Networking.TARGET) target: String
    ): MessageResponse

    @PUT(Const.Networking.API_UPDATE_MESSAGE)
    suspend fun editMessage(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ID) messageId: Int,
        @Body jsonObject: JsonObject
    ): MessageResponse
}