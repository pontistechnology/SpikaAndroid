package com.clover.studio.spikamessenger.utils.helpers

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RestOperations {

    @JvmStatic
    fun <T, A> performGetOperation(
        databaseQuery: () -> LiveData<T>,
        networkCall: suspend () -> Resource<A>,
        saveCallResult: suspend (A) -> Unit
    ): LiveData<Resource<T>> =
        liveData(Dispatchers.IO) {
            emit(Resource.loading())
            val source = databaseQuery.invoke().map { Resource.success(it) }
            emitSource(source)

            val responseStatus = networkCall.invoke()
            if (responseStatus.status == Resource.Status.SUCCESS) {
                saveCallResult(responseStatus.responseData!!)

            } else if (responseStatus.status == Resource.Status.ERROR) {
                emit(Resource.error(responseStatus.message!!))
                emitSource(source)
            }
        }

    @JvmStatic
    fun <T> queryDatabase(
        databaseQuery: () -> LiveData<T>
    ): LiveData<Resource<T>> =
        liveData(Dispatchers.IO) {
            emit(Resource.loading())
            val source = databaseQuery.invoke().map { Resource.success(it) }
            emitSource(source)
        }

    @JvmStatic
    suspend fun <T> queryDatabaseCoreData(
        databaseQuery: suspend () -> T
    ): Resource<T> {
        return try {
            val result = withContext(Dispatchers.IO) {
                databaseQuery.invoke()
            }
            Resource.success(result)
        } catch (e: Exception) {
            Resource.error(e.message ?: "Error", null)
        }
    }

    suspend fun <R> performRestOperation(
        networkCall: suspend () -> Resource<R>
    ): Resource<R> {
        val responseStatus = networkCall.invoke()
        return when (responseStatus.status) {
            Resource.Status.SUCCESS -> {
                Resource.success(responseStatus.responseData!!)
            }
            Resource.Status.TOKEN_EXPIRED -> {
                Resource.tokenExpired(responseStatus.message!!)
            }
            else -> {
                Resource.error(responseStatus.message!!)
            }
        }
    }

    @JvmStatic
    suspend fun <R> performRestOperation(
        networkCall: suspend () -> Resource<R>,
        saveCallResult: (suspend ((R) -> Unit))? = null
    ): Resource<R> {
        val responseStatus = networkCall.invoke()
        return when (responseStatus.status) {
            Resource.Status.SUCCESS -> {
                saveCallResult?.invoke(responseStatus.responseData!!)
                Resource.success(responseStatus.responseData!!)
            }
            Resource.Status.TOKEN_EXPIRED -> {
                Resource.tokenExpired(responseStatus.message!!)
            }
            else -> {
                Resource.error(responseStatus.message!!)
            }
        }
    }


    @JvmStatic
    suspend fun <R> performRestOperationWithStoring(
        networkCall: suspend () -> Resource<R>,
        saveCallResult: suspend (R) -> Unit
    ) {
        val responseStatus = networkCall.invoke()
        when (responseStatus.status) {
            Resource.Status.SUCCESS -> {
                saveCallResult.invoke(responseStatus.responseData!!)
            }
        }
    }
}