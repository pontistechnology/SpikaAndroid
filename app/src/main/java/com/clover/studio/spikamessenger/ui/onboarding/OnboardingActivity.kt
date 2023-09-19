package com.clover.studio.spikamessenger.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.findNavController
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.ActivityOnboardingBinding
import com.clover.studio.spikamessenger.ui.onboarding.terms_and_conditions.TermsAndConditionsDirections
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.extendables.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

fun startOnboardingActivity(fromActivity: Activity, goAccountCreation: Boolean) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, OnboardingActivity::class.java)
        intent.putExtra(Const.Navigation.GO_ACCOUNT_CREATION, goAccountCreation)
        startActivity(intent)
        finishAffinity()
    }

@AndroidEntryPoint
class OnboardingActivity : BaseActivity() {

    private var goToAccountCreation: Boolean = false

    private lateinit var bindingSetup: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityOnboardingBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        goToAccountCreation = intent.getBooleanExtra(Const.Navigation.GO_ACCOUNT_CREATION, false)

        if (goToAccountCreation) {
            findNavController(R.id.container).navigate(TermsAndConditionsDirections.actionSplashFragmentToAccountCreationFragment())
        }

        // TODO check if terms accepted and navigate to registration
    }
}
