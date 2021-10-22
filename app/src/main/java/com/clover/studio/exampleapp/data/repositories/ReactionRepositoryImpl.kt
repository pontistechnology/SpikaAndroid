package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.ReactionDao
import com.clover.studio.exampleapp.data.models.Reaction
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class ReactionRepositoryImpl @Inject constructor(
    private val retrofitService: RetrofitService,
    private val reactionDao: ReactionDao
) : ReactionRepository {
    override suspend fun getReaction(): Reaction {
        TODO("Not yet implemented")
    }
}

interface ReactionRepository {
    suspend fun getReaction(): Reaction
}