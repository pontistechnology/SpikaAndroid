package com.clover.studio.exampleapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {
    val tokenExpiredListener: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    fun setTokenExpiredTrue() {
        tokenExpiredListener.postValue(true)
    }

    fun setTokenExpiredFalse() {
        tokenExpiredListener.postValue(false)
    }
}