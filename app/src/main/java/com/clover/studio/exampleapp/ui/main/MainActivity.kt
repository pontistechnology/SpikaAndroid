package com.clover.studio.exampleapp.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clover.studio.exampleapp.databinding.ActivityMainBinding
import com.clover.studio.exampleapp.ui.main.call_history.CallHistoryFragment
import com.clover.studio.exampleapp.ui.main.chat.ChatFragment
import com.clover.studio.exampleapp.ui.main.contacts.ContactsFragment
import com.clover.studio.exampleapp.ui.main.settings.SettingsFragment
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

fun startMainActivity(fromActivity: Activity) = fromActivity.apply {
    startActivity(Intent(fromActivity as Context, MainActivity::class.java))
    finish()
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var bindingSetup: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityMainBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        initializePager()
        initializeObservers()
    }

    private fun initializePager() {
        val fragmentList = arrayListOf(
            ContactsFragment(),
            CallHistoryFragment(),
            ChatFragment(),
            SettingsFragment()
        )
        val pagerAdapter =
            MainPagerAdapter(baseContext, fragmentList, supportFragmentManager, lifecycle)

        bindingSetup.viewPager.adapter = pagerAdapter

        TabLayoutMediator(bindingSetup.tabLayout, bindingSetup.viewPager) { tab, position ->
            val customTabView: View = pagerAdapter.getTabView(position)
            tab.customView = customTabView
        }.attach()
    }

    private fun initializeObservers() {
        viewModel.getLocalUsers().observe(this) {
            // TODO add logic for local data UI handle
        }
    }
}