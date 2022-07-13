package com.clover.studio.exampleapp.utils.helpers

import android.content.Context
import com.clover.studio.exampleapp.data.models.UserAndPhoneUser
import timber.log.Timber
import java.text.Collator

object Extensions {
    fun MutableList<UserAndPhoneUser>.sortUsersByLocale(context: Context): List<UserAndPhoneUser> {
        for (user in this) {
            Timber.d("${user.user.displayName}, ${user.phoneUser?.name}")
            if (user.user.displayName == null && user.phoneUser?.name == null) {
                this.remove(user)
            }
        }

        val locale = context.resources.configuration.locales.get(0)
        val collator = Collator.getInstance(locale)
        return this.toList().sortedWith(compareBy(collator) {
            it.phoneUser?.name?.lowercase() ?: it.user.displayName?.lowercase()
        })
    }
}