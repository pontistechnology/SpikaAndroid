package com.clover.studio.exampleapp.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.clover.studio.exampleapp.databinding.ActivityMainBinding
import com.clover.studio.exampleapp.utils.extendables.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

fun startMainActivity(fromActivity: Activity) = fromActivity.apply {
    startActivity(Intent(fromActivity as Context, MainActivity::class.java))
    finish()
}

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var bindingSetup: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityMainBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        initializeObservers()
    }

    private fun initializeObservers() {
        viewModel.getLocalUsers().observe(this) {
            // TODO add logic for local data UI handle
        }
    }
}