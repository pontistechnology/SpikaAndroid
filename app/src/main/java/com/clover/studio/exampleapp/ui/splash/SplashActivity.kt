package com.clover.studio.exampleapp.ui.splash

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clover.studio.exampleapp.databinding.ActivitySplashBinding
import com.clover.studio.exampleapp.ui.onboarding.startOnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()

    private lateinit var bindingSetup: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivitySplashBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        goToOnboarding()
    }

    private fun goToOnboarding() {
        val timer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Timber.d("Timer tick $millisUntilFinished")
            }

            override fun onFinish() {
                startOnboardingActivity(this@SplashActivity)
            }
        }
        timer.start()
    }
}