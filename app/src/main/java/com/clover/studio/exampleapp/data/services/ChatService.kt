package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.networking.*
import com.clover.studio.exampleapp.data.models.networking.responses.*
import com.clover.studio.exampleapp.utils.Const
import com.google.gson.JsonObject
import retrofit2.http.*

interface ChatService {
    // Message section
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

    @PUT(Const.Networking.API_UPDATE_MESSAGE)
    suspend fun editMessage(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ID) messageId: Int,
        @Body jsonObject: JsonObject
    ): MessageResponse

    @DELETE(Const.Networking.API_UPDATE_MESSAGE)
    suspend fun deleteMessage(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ID) messageId: Int,
        @Query(Const.Networking.TARGET) target: String
    ): MessageResponse
    // End Message section

    // Reaction section
    @POST(Const.Networking.API_POST_REACTION)
    suspend fun postReaction(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    )

    @DELETE(Const.Networking.API_DELETE_REACTION)
    suspend fun deleteReaction(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ID) id: Int
    )
    // End Reaction section

    // Room section
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

    @DELETE(Const.Networking.API_UPDATE_ROOM)
    suspend fun deleteRoom(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ROOM_ID) roomId: Int
    ): RoomResponse

    @POST(Const.Networking.API_LEAVE_ROOM)
    suspend fun leaveRoom(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ID) roomId: Int
    ): RoomResponse
    // End Room section

    // Notes section
    @GET(Const.Networking.API_NOTES)
    suspend fun getRoomNotes(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ROOM_ID) roomId: Int
    ): NotesResponse

    @POST(Const.Networking.API_NOTES)
    suspend fun createNote(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ROOM_ID) roomId: Int,
        @Body newNote: NewNote
    ): NotesResponse

    @DELETE(Const.Networking.API_MANAGE_NOTE)
    suspend fun deleteNote(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ID) noteId: Int
    ): NotesResponse

    @PUT(Const.Networking.API_MANAGE_NOTE)
    suspend fun updateNote(
        @HeaderMap headers: Map<String, String?>,
        @Path(Const.Networking.ID) noteId: Int,
        @Body newNote: NewNote
    ): NotesResponse
    // End Notes section
}