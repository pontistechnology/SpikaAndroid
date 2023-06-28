package com.clover.studio.spikamessenger.data.services

import com.clover.studio.spikamessenger.data.models.networking.responses.AuthResponse
import com.clover.studio.spikamessenger.utils.Const
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface OnboardingService {
    @POST(value = Const.Networking.API_AUTH)
    suspend fun sendUserData(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): Response<AuthResponse>

    @POST(value = Const.Networking.API_VERIFY_CODE)
    suspend fun verifyUserCode(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): Response<AuthResponse>

    @FormUrlEncoded
    @POST(value = Const.Networking.API_CONTACTS)
    suspend fun sendContacts(
        @HeaderMap headers: Map<String, String?>,
        @Field(Const.Networking.CONTACTS) contacts: List<String>
    ): Response<AuthResponse>

    @PUT(value = Const.Networking.API_UPDATE_USER)
    suspend fun updateUser(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): Response<AuthResponse>
}