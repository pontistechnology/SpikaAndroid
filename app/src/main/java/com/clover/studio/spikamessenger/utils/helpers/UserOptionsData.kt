package com.clover.studio.spikamessenger.utils.helpers

import android.graphics.drawable.Drawable

data class UserOptionsData(
    val option: String,
    val firstDrawable: Drawable?,
    var secondDrawable: Drawable?,
    val switchOption : Boolean,
    var isSwitched: Boolean,
)
