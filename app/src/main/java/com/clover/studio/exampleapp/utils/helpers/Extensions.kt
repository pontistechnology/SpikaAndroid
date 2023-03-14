package com.clover.studio.exampleapp.utils.helpers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.clover.studio.exampleapp.data.models.entity.UserAndPhoneUser
import timber.log.Timber
import java.text.Collator

object Extensions {
    fun MutableList<UserAndPhoneUser>.sortUsersByLocale(context: Context): List<UserAndPhoneUser> {
        val itemsToRemove: MutableList<UserAndPhoneUser> = mutableListOf()
        for (user in this) {
            Timber.d("${user.user.displayName}, ${user.phoneUser?.name}")
            if (user.user.displayName == null && user.phoneUser?.name == null) {
                itemsToRemove.add(user)
            }
        }

        for (item in itemsToRemove) {
            this.remove(item)
        }

        val locale = context.resources.configuration.locales.get(0)
        val collator = Collator.getInstance(locale)
        return this.toList().sortedWith(compareBy(collator) {
            it.phoneUser?.name?.lowercase() ?: it.user.displayName?.lowercase()
        })
    }

    fun <T> LiveData<T>.getDistinct(): LiveData<T> {
        val distinctLiveData = MediatorLiveData<T>()
        distinctLiveData.addSource(this, object : Observer<T> {
            private var initialized = false
            private var lastObj: T? = null

            override fun onChanged(obj: T?) {
                if (!initialized) {
                    initialized = true
                    lastObj = obj
                    distinctLiveData.postValue(lastObj!!)
                } else if ((obj == null && lastObj != null)
                    || obj != lastObj
                ) {
                    lastObj = obj
                    distinctLiveData.postValue(lastObj!!)
                }
            }
        })
        return distinctLiveData
    }
}