package com.clover.studio.exampleapp.utils.extendables

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {

    private fun showProgress(isCancelable: Boolean) {
        if (activity is BaseActivity) {
            (activity as BaseActivity).showProgress(isCancelable)
        }
    }

}