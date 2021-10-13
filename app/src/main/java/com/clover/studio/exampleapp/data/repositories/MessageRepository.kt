package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.MessageDao
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class MessageRepository @Inject constructor(
    private val retrofitService: RetrofitService,
    private val messageDao: MessageDao
) {
}