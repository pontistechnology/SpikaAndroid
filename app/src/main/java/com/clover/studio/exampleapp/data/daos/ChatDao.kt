package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.Chat

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: Chat): Long

    @Query("SELECT * FROM chat")
    fun getChats(): LiveData<List<Chat>>

    @Query("SELECT * FROM chat WHERE id LIKE :chatId LIMIT 1")
    fun getChatById(chatId: String): LiveData<Chat>

    @Delete
    suspend fun deleteChat(chat: Chat)

    @Query("DELETE FROM chat")
    suspend fun removeChats()
}