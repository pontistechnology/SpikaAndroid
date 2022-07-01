package com.clover.studio.exampleapp.utils.extendables

import android.app.AlertDialog
import android.content.Context
import com.clover.studio.exampleapp.R

open class BaseDialog(context: Context?, themeResId: Int = R.style.Theme_Dialog_Dim) : AlertDialog(context, themeResId)