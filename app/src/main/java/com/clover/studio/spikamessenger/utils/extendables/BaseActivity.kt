package com.clover.studio.spikamessenger.utils.extendables

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clover.studio.spikamessenger.BaseViewModel
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.ui.onboarding.startOnboardingActivity
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.dialog.ProgressDialog

open class BaseActivity : AppCompatActivity() {
    // Start: global progress handle
    private var progress: ProgressDialog? = null
    private val viewModel: BaseViewModel by viewModels()

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

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        viewModel.tokenExpiredListener.observe(this, EventObserver { tokenExpired ->
            if (tokenExpired) {
                DialogError.getInstance(this,
                    getString(R.string.warning),
                    getString(R.string.session_expired),
                    null,
                    getString(R.string.ok),
                    object : DialogInteraction {
                        override fun onFirstOptionClicked() {
                            // Ignore
                        }

                        override fun onSecondOptionClicked() {
                            viewModel.setTokenExpiredFalse()
                            viewModel.removeToken()
                            startOnboardingActivity(this@BaseActivity, false)
                        }
                    })
            }
        })
    }
}