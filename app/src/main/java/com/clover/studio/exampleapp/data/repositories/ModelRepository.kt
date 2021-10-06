package com.clover.studio.exampleapp.data.repositories

import com.clover.studio.exampleapp.data.daos.ModelDao
import com.clover.studio.exampleapp.data.services.RetrofitService
import javax.inject.Inject

class ModelRepository @Inject constructor(
    private val retrofitService: RetrofitService,
    private val localModelDao: ModelDao
) {
    fun getModelsLocal() = localModelDao.getAllModels()

    // TODO add remote data handle
}