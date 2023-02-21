package com.clover.studio.exampleapp.ui.main.settings.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import com.clover.studio.exampleapp.databinding.FragmentAppearanceSettingsBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.utils.extendables.BaseFragment

class AppearanceSettings : BaseFragment() {
    private var bindingSetup: FragmentAppearanceSettingsBinding? = null

    private val binding get() = bindingSetup!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentAppearanceSettingsBinding.inflate(inflater, container, false)

        initializeViews()
        initializeListeners()

        return binding.root
    }


    private fun initializeViews() {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressed()
        }

        if (viewModel.getUserTheme() == AppCompatDelegate.MODE_NIGHT_NO){
            binding.ivLightCheckmark.visibility = View.VISIBLE
            binding.ivNightCheckmark.visibility = View.GONE
        } else {
            binding.ivLightCheckmark.visibility = View.GONE
            binding.ivNightCheckmark.visibility = View.VISIBLE
        }


    }

    private fun initializeListeners() {
        binding.clLightTheme.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            binding.ivLightCheckmark.visibility = View.VISIBLE
            binding.ivNightCheckmark.visibility = View.GONE
            viewModel.writeUserTheme(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding.clNightTheme.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            binding.ivLightCheckmark.visibility = View.GONE
            binding.ivNightCheckmark.visibility = View.VISIBLE
            viewModel.writeUserTheme(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}