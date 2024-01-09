package com.clover.studio.spikamessenger.utils.helpers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import java.text.Collator

object Extensions {
    fun MutableList<PrivateGroupChats>.sortChats(context: Context): List<PrivateGroupChats> {
        val locale = context.resources.configuration.locales.get(0)
        val collator = Collator.getInstance(locale)
        // Special conditions for the Bots because they are like private users but without phoneNumber
        return this.toList().sortedWith(compareBy(collator) {
            if (it.phoneNumber != null || it.isBot) {
                it.userName?.lowercase() ?: it.userPhoneName?.lowercase()
            } else {
                it.roomName?.lowercase()
            }
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
                    if (lastObj != null) {
                        distinctLiveData.postValue(lastObj!!)
                    }
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
