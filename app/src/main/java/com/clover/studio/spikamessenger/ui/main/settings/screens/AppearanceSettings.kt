package com.clover.studio.spikamessenger.ui.main.settings.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.FragmentAppearanceSettingsBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.UserOptions
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.helpers.UserOptionsData
import timber.log.Timber

class AppearanceSettings : BaseFragment() {
    private var bindingSetup: FragmentAppearanceSettingsBinding? = null

    private val binding get() = bindingSetup!!

    private var themeOptions: Map<Int, String>? = null
    private var optionList: MutableList<UserOptionsData> = mutableListOf()

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentAppearanceSettingsBinding.inflate(inflater, container, false)

        themeOptions = mapOf(
            0 to Const.Themes.BASIC_THEME_NIGHT,
            1 to Const.Themes.BASIC_THEME,
            2 to Const.Themes.NEON_THEME,
            3 to Const.Themes.NEON_THEME,
            4 to Const.Themes.MINT_THEME
        )

        optionList = mutableListOf(
            UserOptionsData(getString(R.string.theme_dark_marine), null, null, false, false, ""),
            UserOptionsData(getString(R.string.theme_light_marine), null, null, false, false, ""),
            UserOptionsData(getString(R.string.theme_neon), null, null, false, false, ""),
            UserOptionsData(getString(R.string.theme_neon_light), null, null, false, false, ""),
            UserOptionsData(getString(R.string.theme_light_green), null, null, false, false, "")
        )

        initializeViews()

        return binding.root
    }

    private fun initializeViews() = with(binding) {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        getActiveTheme()
        val userOptions = UserOptions(requireContext())
        userOptions.setOptions(optionList)
        userOptions.setOptionsListener(object : UserOptions.OptionsListener {
            override fun clickedOption(option: Int, optionName: String) {
                themeOptions?.get(option)?.let { theme ->
                    viewModel.writeUserTheme(theme)
                    activity?.recreate()
                } ?: run {
                    Timber.d("Not implemented theme option: $option")
                }
            }

            override fun switchOption(optionName: String, isSwitched: Boolean) {
                // Ignore
            }
        })

        binding.flOptionsContainer.addView(userOptions)
    }

    private fun getActiveTheme() {
        // TODO maybe extract this in Tools
        val theme = when (viewModel.getUserTheme()) {
            Const.Themes.MINT_THEME -> getString(R.string.theme_light_green)
            Const.Themes.NEON_THEME -> getString(R.string.theme_neon)
            Const.Themes.BASIC_THEME_NIGHT -> getString(R.string.theme_dark_marine)
            Const.Themes.BASIC_THEME -> getString(R.string.theme_light_marine)
            else -> getString(R.string.theme_light_marine)
        }

        optionList.forEach {
            if (it.option == theme) {
                it.secondDrawable =
                    AppCompatResources.getDrawable(requireContext(), R.drawable.img_checkmark)
            }
        }
    }
}
