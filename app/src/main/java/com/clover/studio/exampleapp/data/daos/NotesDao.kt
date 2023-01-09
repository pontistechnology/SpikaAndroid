package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.entity.Note

@Dao
interface NotesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: List<Note>)

    @Query("SELECT * FROM notes WHERE room_id LIKE :roomId")
    fun getNotesByRoom(roomId: Int): LiveData<List<Note>>

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes")
    suspend fun removeNotes()
}