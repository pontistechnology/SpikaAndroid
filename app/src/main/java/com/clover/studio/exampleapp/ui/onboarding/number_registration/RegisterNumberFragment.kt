package com.clover.studio.exampleapp.ui.onboarding.number_registration

import android.database.Cursor
import android.database.DatabaseUtils
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArraySet
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.FragmentRegisterNumberBinding
import com.clover.studio.exampleapp.ui.onboarding.OnboardingStates
import com.clover.studio.exampleapp.ui.onboarding.OnboardingViewModel
import com.clover.studio.exampleapp.utils.Const
import timber.log.Timber
import java.security.MessageDigest

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
        }

        viewModel.registrationListener.observe(viewLifecycleOwner, {
            when (it) {
                OnboardingStates.REGISTERING_SUCCESS -> {
                    val bundle = bundleOf(
                        Const.Navigation.PHONE_NUMBER to countryCode + binding.etPhoneNumber.text.toString(),
                        Const.Navigation.PHONE_NUMBER_HASHED to hashString(
                            countryCode + binding.etPhoneNumber.text.toString()
                        ),
                        Const.Navigation.COUNTRY_CODE to countryCode.substring(1),
                        Const.Navigation.DEVICE_ID to Settings.Secure.getString(
                            context?.contentResolver,
                            Settings.Secure.ANDROID_ID
                        )
                    )

                    findNavController().navigate(
                        R.id.action_splashFragment_to_verificationFragment, bundle
                    )
                }
                OnboardingStates.REGISTERING_ERROR -> TODO()
                OnboardingStates.CONTACTS_ERROR -> Timber.d("Contacts Error")
                OnboardingStates.CONTACTS_SENT -> Timber.d("Contacts Sent")
                else -> TODO()
            }
        })

        fetchAllUserContacts()
        viewModel.readToken()
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
        val hexChars = "0123456789abcdef"
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

    private fun fetchAllUserContacts() {
        val phoneUserSet: MutableSet<String> = ArraySet()
        val phones: Cursor? = requireActivity().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        while (phones?.moveToNext()!!) {
            val name =
                phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val phoneNumber =
                phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

            val phoneUser = PhoneUser(name, formatE164Number(countryCode, phoneNumber).toString())
            phoneUserSet.add(hashString(formatE164Number(countryCode, phoneNumber).toString()))
            Timber.d("Adding phone user: ${phoneUser.name} ${phoneUser.number}")
        }
        DatabaseUtils.dumpCursor(phones)
        phoneUserSet.forEach { Timber.d("Phone number $it") }

        viewModel.sendContacts(phoneUserSet.toList())
    }

    internal data class PhoneUser(
        val name: String,
        val number: String
    )

    private fun formatE164Number(countryCode: String?, phNum: String?): String? {
        val e164Number: String? = if (TextUtils.isEmpty(countryCode)) {
            phNum
        } else {

            val telephonyManager = getSystemService(requireContext(), TelephonyManager::class.java)
            val isoCode = telephonyManager?.simCountryIso

            Timber.d("Country code: ${isoCode?.uppercase()}")
            PhoneNumberUtils.formatNumberToE164(phNum, isoCode?.uppercase())
        }
        return e164Number
    }
}