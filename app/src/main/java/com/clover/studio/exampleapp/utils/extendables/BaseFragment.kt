package com.clover.studio.exampleapp.utils.extendables

import android.view.View
import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {

    fun showProgress(isCancelable: Boolean) {
        if (activity is BaseActivity) {
            (activity as BaseActivity).showProgress(isCancelable)
        }
    }

    fun hideProgress() {
        if (activity is BaseActivity) {
            (activity as BaseActivity).hideProgress()
        }
    }

    fun showKeyboard(view: View) {
        if (activity is BaseActivity) {
            (activity as BaseActivity).showKeyboard(view)
        }
    }

    fun hideKeyboard(view: View) {
        if (activity is BaseActivity) {
            (activity as BaseActivity).hideKeyboard(view)
        }
    }
}