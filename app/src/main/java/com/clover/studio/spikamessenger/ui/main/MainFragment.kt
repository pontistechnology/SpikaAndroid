package com.clover.studio.spikamessenger.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.FragmentMainBinding
import com.clover.studio.spikamessenger.ui.main.call_history.CallHistoryFragment
import com.clover.studio.spikamessenger.ui.main.contacts.ContactsFragment
import com.clover.studio.spikamessenger.ui.main.rooms.RoomsFragment
import com.clover.studio.spikamessenger.ui.main.settings.SettingsFragment
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.google.android.material.tabs.TabLayoutMediator

class MainFragment : BaseFragment() {
    private var bindingSetup: FragmentMainBinding? = null
    private val viewModel: MainViewModel by activityViewModels()
    private var count: Int = 0
    val icons = arrayOf(
        R.drawable.nav_chat_states,
        R.drawable.nav_call_history_states,
        R.drawable.nav_contact_states,
        R.drawable.nav_settings_states
    )

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
        initializeObservers()
    }

    private fun initializeObservers() {
        viewModel.getRoomsLiveData().observe(viewLifecycleOwner) {
            if (it.responseData != null) {
                count = it.responseData
                initializeTab()
            }
        }
    }

    private fun initializePager() {
        val fragmentList = arrayListOf(
            RoomsFragment(),
            CallHistoryFragment(),
            ContactsFragment(),
            SettingsFragment()
        )

        val pagerAdapter =
            MainPagerAdapter(
                fragmentList,
                this.childFragmentManager,
                lifecycle
            )

        binding.viewPager.adapter = pagerAdapter

        initializeTab()
    }

    private fun initializeTab() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            if (count != 0) {
                if (position == 0) {
                    val badge = tab.orCreateBadge
                    tab.badge?.backgroundColor =
                        ContextCompat.getColor(requireContext(), R.color.style_red)
                    tab.badge?.badgeTextColor =
                        ContextCompat.getColor(requireContext(), R.color.white)
                    badge.number = count
                }
            } else {
                tab.removeBadge()
            }
        }.attach()

        for (i in icons.indices) {
            binding.tabLayout.getTabAt(i)?.setIcon(icons[i])
        }
    }
}
