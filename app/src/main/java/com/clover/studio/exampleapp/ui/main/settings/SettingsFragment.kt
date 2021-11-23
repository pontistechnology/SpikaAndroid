package com.clover.studio.exampleapp.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.clover.studio.exampleapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var bindingSetup: FragmentSettingsBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
}