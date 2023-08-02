package com.clover.studio.spikamessenger.ui.main.settings.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.clover.studio.spikamessenger.databinding.FragmentNotificationSettingsBinding
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment

class NotificationSettingsFragment : BaseFragment() {
    private var bindingSetup: FragmentNotificationSettingsBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentNotificationSettingsBinding.inflate(inflater, container, false)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        return binding.root
    }
}