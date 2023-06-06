package com.clover.studio.spikamessenger.utils.helpers

import com.clover.studio.spikamessenger.utils.Tools
import retrofit2.Response
import timber.log.Timber

const val TOKEN_EXPIRED = 401

// This is not correct anymore, 403 doesn't necessarily mean token invalid
const val TOKEN_INVALID_CODE = 403

abstract class BaseDataSource {
    abstract suspend fun syncContacts(
        contacts: List<String>,
        isLastPage: Boolean
    ): Resource<ContactsSyncResponse>

    protected suspend fun <T> getResult(call: suspend () -> Response<T>): Resource<T> {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) return Resource.success(body)
            } else if (TOKEN_EXPIRED == response.code()) {
                return Resource.tokenExpired("Token expired, user will be logged out of the app")
            }

            return error(" ${response.code()} ${response.message()}")
        } catch (e: Exception) {
            if (Tools.checkError(e)) {
                return Resource.tokenExpired("Token expired, user will be logged out of the app")
            }
            return error(e.message ?: e.toString())
        }
    }

    private fun <T> error(message: String): Resource<T> {
        Timber.d("remoteDataSource $message")
        return Resource.error(
            "Network call has failed for a following reason: $message",
            null
        )
    }
}