package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.utils.Const
import com.google.gson.JsonObject
import retrofit2.http.*

interface OnboardingService {
    @FormUrlEncoded
    @POST(value = Const.Networking.API_AUTH)
    suspend fun sendUserData(
        @Field("telephoneNumber") phoneNumber: String,
        @Field("telephoneNumberHashed") phoneNumberHashed: String,
        @Field("countryCode") countryCode: String,
        @Field("deviceId") deviceId: String
    ): AuthResponse

    @FormUrlEncoded
    @POST(value = Const.Networking.API_VERIFY_CODE)
    suspend fun verifyUserCode(
        @Field("code") code: String,
        @Field("deviceId") deviceId: String
    ): AuthResponse

    @FormUrlEncoded
    @POST(value = Const.Networking.API_CONTACTS)
    suspend fun sendContacts(
        @Header("accesstoken") token: String,
        @Field("contacts") contacts: List<String>
    ): AuthResponse

    @FormUrlEncoded
    @PUT(value = Const.Networking.API_UPDATE_USER)
    suspend fun updateUser(
        @Header("accesstoken") token: String,
        @FieldMap userMap: Map<String, String>
    ): AuthResponse
}