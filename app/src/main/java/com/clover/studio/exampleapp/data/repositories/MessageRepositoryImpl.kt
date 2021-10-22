package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val messageDao: MessageDao
) : MessageRepository {
    override suspend fun getMessage(): Message {
        TODO("Not yet implemented")
    }
}

interface MessageRepository {
    suspend fun getMessage(): Message
}