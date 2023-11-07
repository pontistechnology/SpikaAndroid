package com.clover.studio.spikamessenger.ui.main.settings.screens.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.FragmentPrivacySettingsBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.UserOptions
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.clover.studio.spikamessenger.utils.helpers.UserOptionsData
import timber.log.Timber

class PrivacySettingsFragment : BaseFragment() {
    private var bindingSetup: FragmentPrivacySettingsBinding? = null

    private val binding get() = bindingSetup!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var blockedUserAdapter: BlockedUserAdapter

    private var optionList: MutableList<UserOptionsData> = mutableListOf()

    private var navOptionsBuilder: NavOptions? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentPrivacySettingsBinding.inflate(inflater, container, false)

        navOptionsBuilder = Tools.createCustomNavOptions()

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
                R.id.action_privacySettingsFragment_to_contactDetailsFragment,
                bundle,
                navOptionsBuilder
            )
        }

        binding.rvBlockedUsers.itemAnimator = null
        binding.rvBlockedUsers.adapter = blockedUserAdapter
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvBlockedUsers.layoutManager = layoutManager
    }

    private fun initializeViews() = with(binding) {
        ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        optionList = mutableListOf(
            UserOptionsData(
                option = getString(R.string.blocked_users),
                firstDrawable = null,
                secondDrawable = requireContext().getDrawable(R.drawable.img_arrow_forward),
                additionalText = ""
            ),
            UserOptionsData(
                option = getString(R.string.terms_and_conditions),
                firstDrawable = null,
                secondDrawable = null,
                additionalText = ""
            ),
        )

        val userOptions = UserOptions(requireContext())
        userOptions.setOptions(optionList)
        userOptions.setOptionsListener(object : UserOptions.OptionsListener {
            override fun clickedOption(option: Int, optionName: String) {
                when (optionName) {
                    getString(R.string.blocked_users) -> {
                        binding.flOptionsContainer.removeAllViews()
                        tvPageName.text = getString(R.string.blocked_users)
                        rvBlockedUsers.visibility = View.VISIBLE
                    }

                    getString(R.string.terms_and_conditions) -> {
                        Tools.openTermsAndConditions(requireActivity())
                    }
                }
            }

            override fun switchOption(optionName: String, rotation: Float) {
                // Ignore
            }
        })
        binding.flOptionsContainer.addView(userOptions)
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
