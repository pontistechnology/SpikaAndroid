package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.ChatDao
import com.clover.studio.exampleapp.data.daos.ChatUserDao
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val retrofitService: RetrofitService,
    private val chatDao: ChatDao,
    private val chatUserDao: ChatUserDao
) {
}