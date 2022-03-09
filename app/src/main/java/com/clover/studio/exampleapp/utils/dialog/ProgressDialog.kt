package com.clover.studio.exampleapp.utils.dialog

import android.content.Context
import android.os.Bundle
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.utils.extendables.BaseDialog

class ProgressDialog(context: Context?) : BaseDialog(context, R.style.Theme_Dialog_Dim) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_progress)
    }

    companion object {
        fun ShowProgressDialog(context: Context?, isCancelable: Boolean): ProgressDialog {
            val progressDialog = ProgressDialog(context)
            progressDialog.setCancelable(isCancelable)
            if (progressDialog.window != null) {
                progressDialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
            }
            return progressDialog
        }
    }

    init {
        setCanceledOnTouchOutside(false)
    }
}