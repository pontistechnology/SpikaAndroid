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
import com.clover.studio.spikamessenger.utils.helpers.ColorHelper
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MainFragment : BaseFragment() {
    private var bindingSetup: FragmentMainBinding? = null
    private val viewModel: MainViewModel by activityViewModels()
    private var count: Int = 0
    private val icons = arrayOf(
        R.drawable.img_chat_default,
        R.drawable.img_account_default,
        R.drawable.img_phone,
        R.drawable.img_settings_default
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
            ContactsFragment(),
            CallHistoryFragment(),
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
                    tab.badge?.backgroundColor = ColorHelper.getPrimaryColor(requireContext())
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

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Ignore
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Ignore
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: return
                viewModel.isRoomRefreshing.value = position == 0
            }
        })
    }
}
