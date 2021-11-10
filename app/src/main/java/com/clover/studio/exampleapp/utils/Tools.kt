package com.clover.studio.exampleapp.utils

import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

object Tools {
    fun checkError(ex: Exception) {
        when (ex) {
            is IllegalArgumentException -> Timber.d("IllegalArgumentException")
            is IOException -> Timber.d("IOException")
            is HttpException -> Timber.d("HttpException: ${ex.code()} ${ex.message}")
            else -> Timber.d("UnknownError: ${ex.message}")
        }
    }
}