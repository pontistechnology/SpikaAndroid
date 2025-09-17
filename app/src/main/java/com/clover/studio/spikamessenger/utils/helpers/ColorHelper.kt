package com.clover.studio.spikamessenger.utils.helpers

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
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
    fun getSecondaryColor(context: Context) = getAttributeColor(context, R.attr.secondaryColor)

    fun getPrimaryTextColor(context: Context) = getAttributeColor(context, R.attr.primaryTextColor)

    fun getSecondaryTextColor(context: Context) =
        getAttributeColor(context, R.attr.secondaryTextColor)

    fun getTertiaryTextColor(context: Context) =
        getAttributeColor(context, R.attr.tertiaryTextColor)

    fun getAdditionalColor(context: Context) =
        getAttributeColor(context, R.attr.additionalColor)

    fun getSecondAdditionalColor(context: Context) =
        getAttributeColor(context, R.attr.secondAdditionalColor)

    fun getThirdAdditionalColor(context: Context) =
        getAttributeColor(context, R.attr.thirdAdditionalColor)

    fun getFourthAdditionalColor(context: Context) =
        getAttributeColor(context, R.attr.fourthAdditionalColor)

    fun getFourthAdditionalColorWithAlpha(context: Context): Int {
        val color = getAttributeColor(context, R.attr.fourthAdditionalColor)
        return ColorUtils.setAlphaComponent(color, 60)
    }
}