package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.entity.Reaction

@Dao
interface ReactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reaction: Reaction): Long

    @Query("SELECT * FROM reaction")
    fun getReactions(): LiveData<List<Reaction>>

    @Query("SELECT * FROM reaction WHERE id LIKE :reactionId LIMIT 1")
    fun getReactionById(reactionId: String): LiveData<Reaction>

    @Delete
    suspend fun deleteReaction(reaction: Reaction)

    @Query("DELETE FROM reaction")
    suspend fun removeReactions()
}