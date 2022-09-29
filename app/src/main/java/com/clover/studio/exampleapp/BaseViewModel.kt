package com.clover.studio.exampleapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

open class BaseViewModel : ViewModel() {
    val tokenExpiredListener: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    fun setTokenExpiredTrue() {
        tokenExpiredListener.value = true
        Timber.d("baseViewModel: true, ${tokenExpiredListener.value.toString()}")
    }

    fun setTokenExpiredFalse() {
        tokenExpiredListener.value = false
        Timber.d("baseViewModel: false, ${tokenExpiredListener.value.toString()}")
    }
}