package com.clover.studio.spikamessenger.utils.dialog

import android.content.Context
import android.os.Bundle
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.utils.extendables.BaseDialog

class ProgressDialog(context: Context?, themeResId: Int?) : BaseDialog(context, themeResId) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_progress)
    }

    companion object {
        fun showProgressDialog(context: Context?, isCancelable: Boolean): ProgressDialog {
            val progressDialog = ProgressDialog(context, R.style.Theme_Dialog_Dim)
            progressDialog.setCancelable(isCancelable)
            if (progressDialog.window != null) {
                progressDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
                progressDialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
            }
            return progressDialog
        }
    }

    init {
        setCanceledOnTouchOutside(false)
    }
}