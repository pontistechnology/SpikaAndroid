package com.clover.studio.exampleapp.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clover.studio.exampleapp.databinding.ActivityOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint

fun startOnboardingActivity(fromActivity: Activity) = fromActivity.apply {
    startActivity(Intent(fromActivity as Context, OnboardingActivity::class.java))
    finish()
}

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private val viewModel: OnboardingViewModel by viewModels()

    private lateinit var bindingSetup: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityOnboardingBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)
    }
}