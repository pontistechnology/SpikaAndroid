package com.clover.studio.exampleapp.utils.extendables

import android.app.AlertDialog
import android.content.Context
import com.clover.studio.exampleapp.R

open class BaseDialog(context: Context?) : AlertDialog(context)

interface DialogInteraction {
    fun onFirstOptionClicked() {}
    fun onSecondOptionClicked() {}
    fun onDialogClicked() {}
}