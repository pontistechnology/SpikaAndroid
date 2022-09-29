package com.clover.studio.exampleapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.clover.studio.exampleapp.utils.Event

open class BaseViewModel : ViewModel() {
    val tokenExpiredListener = MutableLiveData<Event<Boolean>>()

    fun setTokenExpiredTrue(event: Event<Boolean>) {
        tokenExpiredListener.postValue(event)
    }

    fun setTokenExpiredFalse() {
        tokenExpiredListener.postValue(Event(false))
    }
}