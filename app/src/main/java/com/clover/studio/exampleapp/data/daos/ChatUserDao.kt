package com.clover.studio.exampleapp.data.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.clover.studio.exampleapp.data.models.ChatUser

@Dao
interface ChatUserDao {

    @Transaction
    @Query("SELECT * from chat")
    suspend fun getChatAndUsers(): List<ChatUser>
}