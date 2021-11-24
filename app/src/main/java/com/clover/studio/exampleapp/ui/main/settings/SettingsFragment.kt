package com.clover.studio.exampleapp.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var bindingSetup: FragmentSettingsBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentSettingsBinding.inflate(inflater, container, false)

        setupClickListeners()

        return binding.root
    }

    private fun setupClickListeners() {
        binding.clPrivacy.setOnClickListener {
            goToPrivacySettings()
        }

        binding.clChat.setOnClickListener {
            goToChatSettings()
        }

        binding.clNotifications.setOnClickListener {
            goToNotificationSettings()
        }

        binding.clMediaDownload.setOnClickListener {
            goToDownloadSettings()
        }
    }

    private fun goToPrivacySettings() {
        findNavController().navigate(R.id.action_mainFragment_to_privacySettingsFragment22)
    }

    private fun goToChatSettings() {
        findNavController().navigate(R.id.action_mainFragment_to_chatSettingsFragment22)
    }

    private fun goToNotificationSettings() {
        findNavController().navigate(R.id.action_mainFragment_to_notificationSettingsFragment22)
    }

    private fun goToDownloadSettings() {
        findNavController().navigate(R.id.action_mainFragment_to_downloadSettingsFragment2)
    }
}