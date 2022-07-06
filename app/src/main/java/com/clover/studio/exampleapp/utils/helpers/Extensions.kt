package com.clover.studio.exampleapp.utils.helpers

import android.content.Context
import com.clover.studio.exampleapp.data.models.UserAndPhoneUser
import java.text.Collator

object Extensions {
    fun List<UserAndPhoneUser>.sortUsersByLocale(context: Context): List<UserAndPhoneUser> {
        val locale = context.resources.configuration.locales.get(0)
        val collator = Collator.getInstance(locale)
        return this.toMutableList().sortedWith(compareBy(collator) {
            it.phoneUser?.name?.lowercase() ?: it.user.displayName?.lowercase()
        })
    }
}