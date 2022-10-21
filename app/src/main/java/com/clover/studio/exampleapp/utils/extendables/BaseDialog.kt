package com.clover.studio.exampleapp.utils.extendables

import android.app.AlertDialog
import android.content.Context
import com.clover.studio.exampleapp.R

open class BaseDialog(context: Context?, themeResId: Int?) : AlertDialog(context) {
    constructor(context: Context?): this(context, null)
}


interface DialogInteraction {
    fun onFirstOptionClicked() {}
    fun onSecondOptionClicked() {}
    fun onDialogClicked() {}
}