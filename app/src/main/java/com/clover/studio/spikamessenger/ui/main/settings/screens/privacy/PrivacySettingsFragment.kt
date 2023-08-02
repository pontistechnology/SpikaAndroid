package com.clover.studio.spikamessenger.ui.main.settings.screens.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.FragmentPrivacySettingsBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.helpers.Resource
import timber.log.Timber

class PrivacySettingsFragment : BaseFragment() {
    private var bindingSetup: FragmentPrivacySettingsBinding? = null

    private val binding get() = bindingSetup!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var blockedUserAdapter: BlockedUserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentPrivacySettingsBinding.inflate(inflater, container, false)

        initializeViews()
        setupAdapter()
        initializeObservers()

        return binding.root
    }

    private fun initializeObservers() {
        viewModel.blockedUserListListener().observe(viewLifecycleOwner) {
            if (it?.isNotEmpty() == true) {
                viewModel.fetchBlockedUsersLocally(it)
            }
        }

        viewModel.blockedListListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData != null) {
                        blockedUserAdapter.submitList(it.responseData)
                    }
                }
                Resource.Status.ERROR -> Timber.d("Failed to fetch blocked users")
                else -> Timber.d("Other error")
            }
        })
    }

    private fun setupAdapter() {
        blockedUserAdapter = BlockedUserAdapter(requireContext()) {
            val bundle = bundleOf(Const.Navigation.USER_PROFILE to it)
            findNavController().navigate(
                R.id.action_privacySettingsFragment2_to_contactDetailsFragment,
                bundle
            )
        }

        binding.rvBlockedUsers.itemAnimator = null
        binding.rvBlockedUsers.adapter = blockedUserAdapter
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvBlockedUsers.layoutManager = layoutManager
    }

    private fun initializeViews() {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.clBlockedUsers.setOnClickListener {
            binding.clBlockedUsers.visibility = View.GONE
            binding.tvPageName.text = getString(R.string.blocked_users)
            binding.rvBlockedUsers.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getBlockedUsersList()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unregisterSharedPrefsReceiver()
    }
}