package com.clover.studio.exampleapp.data.services

import android.provider.ContactsContract
import com.clover.studio.exampleapp.data.models.Model
import retrofit2.Response
import retrofit2.http.GET

interface RetrofitService {
   // implement calls to API
    @GET
    suspend fun getModels() : Response<List<Model>>
}