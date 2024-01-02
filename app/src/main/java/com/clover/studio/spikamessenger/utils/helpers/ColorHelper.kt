package com.clover.studio.spikamessenger.utils.helpers

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import com.clover.studio.spikamessenger.R
import timber.log.Timber

object ColorHelper {

    private fun getAttributeColor(context: Context, attributeId: Int): Int {
        val typedValue = TypedValue()
        try {
            val theme = context.theme
            theme.resolveAttribute(attributeId, typedValue, true)
        } catch (e: Resources.NotFoundException) {
            Timber.d("Resolve attribute exception: $e")
        }
        return typedValue.data
    }

    fun getPrimaryColor(context: Context) = getAttributeColor(context, R.attr.primaryColor)

    fun getPrimaryTextColor(context: Context) = getAttributeColor(context, R.attr.primaryTextColor)

    fun getSecondaryColor(context: Context) = getAttributeColor(context, R.attr.secondaryColor)

    fun getSecondAdditionalColor(context: Context) =
        getAttributeColor(context, R.attr.secondAdditionalColor)

    fun getFourthAdditionalColor(context: Context) =
        getAttributeColor(context, R.attr.fourthAdditionalColor)

    fun getFourthAdditionalColorWithAlpha(context: Context) : Int {
        val color = getAttributeColor(context, R.attr.fourthAdditionalColor)
        return Color.argb(60, Color.red(color), Color.green(color), Color.blue(color))
    }
}