package com.clover.studio.spikamessenger.ui.main.settings.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.clover.studio.spikamessenger.databinding.FragmentHelpBinding
import com.clover.studio.spikamessenger.utils.Tools


class HelpFragment : Fragment() {

    private var bindingSetup: FragmentHelpBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentHelpBinding.inflate(inflater, container, false)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvTerms.setOnClickListener {
            Tools.openTermsAndConditions(requireActivity())
        }

        return binding.root
    }
}