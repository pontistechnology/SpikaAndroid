package com.clover.studio.spikamessenger.ui.main.settings.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import com.clover.studio.spikamessenger.databinding.FragmentAppearanceSettingsBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import timber.log.Timber

class AppearanceSettings : BaseFragment() {
    private var bindingSetup: FragmentAppearanceSettingsBinding? = null

    private val binding get() = bindingSetup!!

    private var listOfCheckMarks = listOf<ImageView>()

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentAppearanceSettingsBinding.inflate(inflater, container, false)

        listOfCheckMarks = listOf(
            binding.ivLilacCheck, binding.ivMintCheck, binding.ivBaseCheck
        )

        initializeViews()
        initializeListeners()

        return binding.root
    }


    private fun initializeViews() = with(binding) {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        when (viewModel.getUserTheme()) {
            Const.Themes.MINT_THEME -> {
                Timber.d("Mint theme")
                setThemeChecks(ivMintCheck)
            }

            Const.Themes.NEON_THEME -> {
                Timber.d("Neon theme")
                setThemeChecks(ivLilacCheck)
            }

            else -> {
                Timber.d("Base theme")
                setThemeChecks(ivBaseCheck)
            }
        }
    }

    private fun initializeListeners() = with(binding) {
        // TODO change user theme:

        clBaseTheme.setOnClickListener {
            Timber.d("Base theme click")
            viewModel.writeUserTheme(Const.Themes.BASIC_THEME)
            setThemeChecks(ivBaseCheck)
            activity?.recreate()
        }

        clMintTheme.setOnClickListener {
            Timber.d("Mint theme click")
            viewModel.writeUserTheme(Const.Themes.MINT_THEME)
            activity?.recreate()
        }

        clLilacTheme.setOnClickListener {
            Timber.d("Neon theme click")
            viewModel.writeUserTheme(Const.Themes.NEON_THEME)
            setThemeChecks(ivLilacCheck)
            activity?.recreate()
        }

        Timber.d("User theme: ${viewModel.getUserTheme()}")
    }

    private fun setThemeChecks(checkmark: ImageView) {
        listOfCheckMarks.forEach {
            it.visibility = if (checkmark == it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }
}
