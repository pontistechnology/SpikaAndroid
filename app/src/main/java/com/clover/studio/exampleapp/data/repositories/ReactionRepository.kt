package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.ReactionDao
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class ReactionRepository @Inject constructor(
    private val retrofitService: RetrofitService,
    private val reactionDao: ReactionDao
) {
}