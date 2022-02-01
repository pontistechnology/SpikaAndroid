package com.clover.studio.exampleapp.data.services

import com.clover.studio.exampleapp.data.models.networking.AuthResponse
import com.clover.studio.exampleapp.utils.Const
import retrofit2.http.*

interface OnboardingService {
    @FormUrlEncoded
    @POST(value = Const.Networking.API_AUTH)
    suspend fun sendUserData(
        @HeaderMap headers: Map<String, String?>,
        @Field("telephoneNumber") phoneNumber: String,
        @Field("telephoneNumberHashed") phoneNumberHashed: String,
        @Field("countryCode") countryCode: String,
        @Field("deviceId") deviceId: String
    ): AuthResponse

    @FormUrlEncoded
    @POST(value = Const.Networking.API_VERIFY_CODE)
    suspend fun verifyUserCode(
        @HeaderMap headers: Map<String, String?>,
        @Field("code") code: String,
        @Field("deviceId") deviceId: String
    ): AuthResponse

    @FormUrlEncoded
    @POST(value = Const.Networking.API_CONTACTS)
    suspend fun sendContacts(
        @HeaderMap headers: Map<String, String?>,
        @Field("contacts") contacts: List<String>
    ): AuthResponse

    @FormUrlEncoded
    @PUT(value = Const.Networking.API_UPDATE_USER)
    suspend fun updateUser(
        @HeaderMap headers: Map<String, String?>,
        @FieldMap userMap: Map<String, String>
    ): AuthResponse

    @FormUrlEncoded
    @POST(value = Const.Networking.API_UPLOAD_FILE)
    suspend fun uploadFiles(
        @HeaderMap headers: Map<String, String?>,
        @Field("chunk") chunk: String,
        @Field("offset") offset: Long,
        @Field("total") total: Long,
        @Field("size") size: Long,
        @Field("mimeType") mimeType: String,
        @Field("fileName") fileName: String,
        @Field("type") type: String?,
        @Field("fileHash") fileHash: String?,
        @Field("relationId") relationId: Int?,
        @Field("clientId") clientId: String?
    )
}