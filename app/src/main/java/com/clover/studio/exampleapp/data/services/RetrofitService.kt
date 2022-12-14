package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.networking.*
import com.clover.studio.exampleapp.utils.Const
import com.google.gson.JsonObject
import retrofit2.http.*

interface RetrofitService {
    @GET(Const.Networking.API_GET_ROOM_BY_ID)
    suspend fun getRoomById(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.USER_ID) userId: Int
    ): RoomResponse

    @POST(Const.Networking.API_POST_NEW_ROOM)
    suspend fun createNewRoom(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): RoomResponse

    @PUT(Const.Networking.API_UPDATE_TOKEN)
    suspend fun updatePushToken(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    )

    @PUT(value = Const.Networking.API_UPDATE_USER)
    suspend fun updateUser(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): AuthResponse

    @POST(value = Const.Networking.API_UPLOAD_FILE)
    suspend fun uploadFiles(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): FileResponse

    @POST(value = Const.Networking.API_VERIFY_FILE)
    suspend fun verifyFile(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): FileResponse

    @PUT(Const.Networking.API_UPDATE_ROOM)
    suspend fun updateRoom(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject,
        @Path(Const.Networking.ROOM_ID) roomId: Int
    ): RoomResponse

    @GET(Const.Networking.API_GET_SETTINGS)
    suspend fun getSettings(
        @HeaderMap headers: Map<String, String?>
    ): SettingsResponse
}