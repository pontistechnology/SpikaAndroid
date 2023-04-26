package com.clover.studio.exampleapp.utils.helpers

import com.clover.studio.exampleapp.utils.Tools
import retrofit2.Response
import timber.log.Timber

const val TOKEN_EXPIRED = 401
const val TOKEN_INVALID_CODE = 403

abstract class BaseDataSource {

    protected suspend fun <T> getResult(call: suspend () -> Response<T>): Resource<T> {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) return Resource.success(body)
            }
            else if (TOKEN_EXPIRED == response.code() || TOKEN_INVALID_CODE == response.code() ){
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