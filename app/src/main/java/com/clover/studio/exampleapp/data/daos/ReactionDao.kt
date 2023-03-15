package com.clover.studio.exampleapp.data.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.clover.studio.exampleapp.data.models.entity.Reaction
import com.clover.studio.exampleapp.utils.helpers.Extensions.getDistinct

@Dao
interface ReactionDao: BaseDao<Reaction> {
    @Query("SELECT * FROM reaction")
    fun getReactions(): LiveData<List<Reaction>>

    @Query("SELECT * FROM reaction WHERE id LIKE :reactionId LIMIT 1")
    fun getReactionById(reactionId: String): LiveData<Reaction>

    fun getDistinctReactionById(reactionId: String): LiveData<Reaction> =
        getReactionById(reactionId).getDistinct()

    @Query("DELETE FROM reaction")
    suspend fun removeReactions()
}