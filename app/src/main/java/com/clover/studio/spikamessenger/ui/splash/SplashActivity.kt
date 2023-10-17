package com.clover.studio.spikamessenger.ui.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.viewModels
import com.clover.studio.spikamessenger.databinding.ActivitySplashBinding
import com.clover.studio.spikamessenger.ui.main.startMainActivity
import com.clover.studio.spikamessenger.ui.onboarding.startOnboardingActivity
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.extendables.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    private val viewModel: SplashViewModel by viewModels()

    private lateinit var bindingSetup: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivitySplashBinding.inflate(layoutInflater)

        // TODO add custom splash for themes

        val view = bindingSetup.root

        setContentView(view)

        checkLoginInformation()
    }

    private fun checkLoginInformation() {
        viewModel.splashTokenListener.observe(this, EventObserver {
            when (it) {
                SplashStates.NAVIGATE_ONBOARDING -> goToOnboarding()
                SplashStates.NAVIGATE_MAIN -> goToMainActivity()
                SplashStates.NAVIGATE_ACCOUNT_CREATION -> goToAccountCreation()
                else -> Timber.d("Some error")
            }
        })

        viewModel.checkToken()
    }

    private fun goToAccountCreation() {
        startTimerAndNavigate { startOnboardingActivity(this@SplashActivity, true) }
    }

    private fun goToOnboarding() {
        startTimerAndNavigate { startOnboardingActivity(this@SplashActivity, false) }
    }

    private fun goToMainActivity() {
        startMainActivity(this@SplashActivity)
    }

    private fun startTimerAndNavigate(location: () -> Unit) {
        val timer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Timber.d("Timer tick $millisUntilFinished")
            }

            override fun onFinish() {
                location()
            }
        }
        timer.start()
    }
}
