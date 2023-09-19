package com.clover.studio.spikamessenger.utils.extendables

import android.app.AlertDialog
import android.content.Context

open class BaseDialog(context: Context?, themeResId: Int?) : AlertDialog(context) {
    constructor(context: Context?): this(context, null)
}


interface DialogInteraction {
    fun onFirstOptionClicked() {}
    fun onSecondOptionClicked() {}
}
