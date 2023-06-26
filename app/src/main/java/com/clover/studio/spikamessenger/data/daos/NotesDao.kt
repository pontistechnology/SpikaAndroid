package com.clover.studio.spikamessenger.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.spikamessenger.data.models.entity.Note

@Dao
interface NotesDao : BaseDao<Note> {
    @Query("SELECT * FROM notes WHERE room_id LIKE :roomId")
    fun getNotesByRoom(roomId: Int): LiveData<List<Note>>

    @Query("DELETE FROM notes WHERE id LIKE :noteId")
    suspend fun deleteNote(noteId: Int)

    @Query("DELETE FROM notes")
    suspend fun removeNotes()
}