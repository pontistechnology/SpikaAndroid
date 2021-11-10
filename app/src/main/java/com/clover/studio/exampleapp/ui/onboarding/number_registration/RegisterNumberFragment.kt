package com.clover.studio.exampleapp.ui.onboarding.number_registration

import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.FragmentRegisterNumberBinding
import com.clover.studio.exampleapp.ui.onboarding.OnboardingViewModel
import com.clover.studio.exampleapp.utils.Const
import dagger.hilt.android.AndroidEntryPoint
import java.security.MessageDigest

@AndroidEntryPoint
class RegisterNumberFragment : Fragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var countryCode: String

    private var bindingSetup: FragmentRegisterNumberBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        countryCode = if (arguments?.getString(Const.Navigation.COUNTRY_CODE).isNullOrEmpty())
            getString(R.string.country_code_placeholder)
        else
            requireArguments().getString(Const.Navigation.COUNTRY_CODE).toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        bindingSetup = FragmentRegisterNumberBinding.inflate(inflater, container, false)

        setTextListener()

        if (countryCode != "") {
            binding.tvCountryCode.text = countryCode
        }

        binding.tvCountryCode.setOnClickListener {
            findNavController().navigate(
                R.id.action_splashFragment_to_countryPickerFragment
            )
        }

        binding.btnNext.setOnClickListener {
            viewModel.sendNewUserData(
                countryCode + binding.etPhoneNumber.text.toString(),
                hashString(
                    countryCode + binding.etPhoneNumber.text.toString()
                ),
                countryCode.substring(1),
                Settings.Secure.getString(
                    context?.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
            )

            val bundle = bundleOf(
                Const.Navigation.PHONE_NUMBER to countryCode + binding.etPhoneNumber.text.toString()
            )

            findNavController().navigate(
                R.id.action_splashFragment_to_verificationFragment, bundle
            )
        }
        return binding.root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingSetup = null
    }

    private fun setTextListener() {
        binding.etPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                binding.btnNext.isEnabled = s.isNotEmpty()
            }
        })
    }

    private fun hashString(input: String): String {
        val hexChars = "0123456789ABCDEF"
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }

        return result.toString()
    }
}