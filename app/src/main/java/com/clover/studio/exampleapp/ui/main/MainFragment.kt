package com.clover.studio.exampleapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clover.studio.exampleapp.databinding.FragmentMainBinding
import com.clover.studio.exampleapp.ui.main.call_history.CallHistoryFragment
import com.clover.studio.exampleapp.ui.main.contacts.ContactsFragment
import com.clover.studio.exampleapp.ui.main.rooms.RoomsFragment
import com.clover.studio.exampleapp.ui.main.settings.SettingsFragment
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainFragment : BaseFragment() {
    private var bindingSetup: FragmentMainBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentMainBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializePager()
    }

    private fun initializePager() {
        val fragmentList = arrayListOf(
            ContactsFragment(),
            CallHistoryFragment(),
            RoomsFragment(),
            SettingsFragment()
        )

        val pagerAdapter =
            MainPagerAdapter(
                requireContext(),
                fragmentList,
                this.childFragmentManager,
                lifecycle
            )

        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val customTabView: View = pagerAdapter.getTabView(position)
            tab.customView = customTabView
        }.attach()
    }
}