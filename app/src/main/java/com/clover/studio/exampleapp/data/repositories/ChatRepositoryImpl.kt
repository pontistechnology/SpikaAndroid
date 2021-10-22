package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.ChatDao
import com.clover.studio.exampleapp.data.daos.ChatUserDao
import com.clover.studio.exampleapp.data.models.Chat
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val chatDao: ChatDao,
    private val chatUserDao: ChatUserDao
) : ChatRepository {
    override suspend fun getChatData(): Chat {
        TODO("Not yet implemented")
    }
}

interface ChatRepository {
    suspend fun getChatData(): Chat
}