package com.clover.studio.exampleapp.utils.extendables

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {
    fun showProgress() {
        showProgress(true)
    }

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
}