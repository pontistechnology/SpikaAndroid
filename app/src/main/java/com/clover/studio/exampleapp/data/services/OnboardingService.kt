package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.utils.Const
import com.google.gson.JsonObject
import retrofit2.http.*

interface OnboardingService {
    @POST(value = Const.Networking.API_AUTH)
    suspend fun sendUserData(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): AuthResponse

    @POST(value = Const.Networking.API_VERIFY_CODE)
    suspend fun verifyUserCode(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): AuthResponse

    @FormUrlEncoded
    @POST(value = Const.Networking.API_CONTACTS)
    suspend fun sendContacts(
        @HeaderMap headers: Map<String, String?>,
        @Field(Const.Networking.CONTACTS) contacts: List<String>
    ): AuthResponse

    @PUT(value = Const.Networking.API_UPDATE_USER)
    suspend fun updateUser(
        @HeaderMap headers: Map<String, String?>,
        @Body jsonObject: JsonObject
    ): AuthResponse
}