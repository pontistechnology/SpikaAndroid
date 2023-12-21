package com.clover.studio.spikamessenger.utils.helpers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import java.text.Collator

object Extensions {
    fun MutableList<PrivateGroupChats>.sortPrivateChats(context: Context): List<PrivateGroupChats> {
        val locale = context.resources.configuration.locales.get(0)
        val collator = Collator.getInstance(locale)
        return this.toList().sortedWith(compareBy(collator) {
            it.userName?.lowercase()
                ?: it.roomName?.lowercase()
        })
    }

    // TODO group sorting (if needed)
//    fun MutableList<PrivateGroupChats>.sortGroupChats(context: Context): List<PrivateGroupChats> {
//        val locale = context.resources.configuration.locales.get(0)
//        val collator = Collator.getInstance(locale)
//        return this.toList().sortedWith(compareBy(collator) {
//            it.group!!.room.name
//        })
//    }


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
