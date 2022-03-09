package com.clover.studio.exampleapp.utils.extendables

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface

open class BaseDialog : AlertDialog {
    protected constructor(context: Context?) : super(context)
    constructor(context: Context?, themeResId: Int) : super(context, themeResId)
    constructor(
        context: Context?,
        cancelable: Boolean,
        cancelListener: DialogInterface.OnCancelListener?
    ) : super(context, cancelable, cancelListener)
}