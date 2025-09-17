package com.clover.studio.spikamessenger.utils.helpers

import android.graphics.drawable.Drawable

data class UserOptionsData(
    var option: String = "",
    val firstDrawable: Drawable? = null,
    var secondDrawable: Drawable? = null,
    val switchOption : Boolean = false,
    var isSwitched: Boolean = false,
)
