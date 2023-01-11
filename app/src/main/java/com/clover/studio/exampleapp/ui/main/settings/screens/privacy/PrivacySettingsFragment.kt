package com.clover.studio.exampleapp.ui.main.settings.screens.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.FragmentPrivacySettingsBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
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
//        viewModel.blockedUserListListener().observe(viewLifecycleOwner) {
//            if (it?.isNotEmpty()) {
//                blockedUserAdapter.submitList(it)
//            } else Timber.d("Failed to fetch blocked users")
//        }

        viewModel.getBlockedUsersList()
    }

    private fun setupAdapter() {
        blockedUserAdapter = BlockedUserAdapter(requireContext()) {
            // TODO get user id and send to user details screen
        }

        binding.rvBlockedUsers.itemAnimator = null
        binding.rvBlockedUsers.adapter = blockedUserAdapter
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvBlockedUsers.layoutManager = layoutManager
    }

    private fun initializeViews() {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.clBlockedUsers.setOnClickListener {
            binding.clBlockedUsers.visibility = View.GONE
            binding.tvPageName.text = getString(R.string.blocked_users)
            binding.rvBlockedUsers.visibility = View.VISIBLE
        }
    }
}