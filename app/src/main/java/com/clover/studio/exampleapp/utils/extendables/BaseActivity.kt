package com.clover.studio.exampleapp.utils.extendables

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.clover.studio.exampleapp.utils.dialog.ProgressDialog

open class BaseActivity : AppCompatActivity() {
    // start: global progress handle
    private var progress: ProgressDialog? = null

    @JvmOverloads
    fun showProgress(isCancelable: Boolean = true) {
        try {
            if (progress == null || !progress!!.isShowing) {
                progress = ProgressDialog.showProgressDialog(this, isCancelable)
                progress!!.show()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun hideProgress() {
        try {
            if (progress != null && progress!!.isShowing) {
                progress!!.dismiss()
            }
            progress = null
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun showKeyboard(view: View) {
        view.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideKeyboard(view: View) {
        val inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}