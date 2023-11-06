package com.clover.studio.spikamessenger.ui.main.settings.screens

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.clover.studio.spikamessenger.databinding.FragmentAppearanceSettingsBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.UserOptions
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import timber.log.Timber

class AppearanceSettings : BaseFragment() {
    private var bindingSetup: FragmentAppearanceSettingsBinding? = null

    private val binding get() = bindingSetup!!

    private var themeOptions: Map<Int, String>? = null

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

        initializeViews()

        return binding.root
    }


    private fun initializeViews() = with(binding) {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        val userOptions = UserOptions(requireContext())
        val optionList = mutableListOf<Pair<String, Drawable?>>(
            Pair("Dark Marine", null),
            Pair("Light Marine", null),
            Pair("Neon", null),
            Pair("Neon Light", null),
            Pair("Light Green", null),
        )
        userOptions.setOptions(optionList)
        userOptions.setOptionsListener(object : UserOptions.OptionsListener {
            override fun clickedOption(optionName: Int) {
                themeOptions?.get(optionName)?.let { theme ->
                    viewModel.writeUserTheme(theme)
                    activity?.recreate()
                } ?: run {
                    Timber.d("Not implemented theme option: $optionName")
                }
            }
        })

        binding.flOptionsContainer.addView(userOptions)
    }

//    private fun setThemeChecks(checkmark: ImageView) {
//        listOfCheckMarks.forEach {
//            it.visibility = if (checkmark == it) {
//                View.VISIBLE
//            } else {
//                View.GONE
//            }
//        }
//    }
}
