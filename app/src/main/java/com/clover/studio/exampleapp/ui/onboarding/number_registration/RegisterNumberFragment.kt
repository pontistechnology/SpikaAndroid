package com.clover.studio.exampleapp.ui.onboarding.number_registration

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.DatabaseUtils
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.collection.ArraySet
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.PhoneUser
import com.clover.studio.exampleapp.databinding.FragmentRegisterNumberBinding
import com.clover.studio.exampleapp.ui.onboarding.OnboardingStates
import com.clover.studio.exampleapp.ui.onboarding.OnboardingViewModel
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools.formatE164Number
import com.clover.studio.exampleapp.utils.Tools.hashString
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.google.gson.JsonObject
import timber.log.Timber

class RegisterNumberFragment : BaseFragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var countryCode: String
    private lateinit var contactsPermission: ActivityResultLauncher<String>

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

        if (countryCode != "") {
            binding.tvCountryCode.text = countryCode
        }

        checkContactsPermission()
        setTextListener()
        setClickListeners()
        setObservers()

        // Log check if token is present in shared prefs
        viewModel.readToken()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingSetup = null
    }

    private fun setObservers() {
        viewModel.registrationListener.observe(viewLifecycleOwner, EventObserver {
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
                else -> TODO()
            }
        })
    }

    private fun setClickListeners() {
        binding.tvCountryCode.setOnClickListener {
            findNavController().navigate(
                R.id.action_splashFragment_to_countryPickerFragment
            )
        }

        binding.btnNext.setOnClickListener {
            viewModel.sendNewUserData(getJsonObject())
        }
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

    private fun getJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty(
            Const.JsonFields.TELEPHONE_NUMBER,
            countryCode + binding.etPhoneNumber.text.toString()
        )
        jsonObject.addProperty(
            Const.JsonFields.TELEPHONE_NUMBER_HASHED, hashString(
                countryCode + binding.etPhoneNumber.text.toString()
            )
        )
        jsonObject.addProperty(Const.JsonFields.COUNTRY_CODE, countryCode.substring(1))
        jsonObject.addProperty(
            Const.JsonFields.DEVICE_ID, Settings.Secure.getString(
                context?.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        )

        return jsonObject
    }

    @SuppressLint("Range")
    private fun fetchAllUserContacts() {
        val phoneUserSet: MutableSet<String> = ArraySet()
        val phoneUsers: MutableList<PhoneUser> = ArrayList()
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

            val phoneUser = PhoneUser(
                name,
                formatE164Number(requireContext(), countryCode, phoneNumber).toString()
            )
            phoneUsers.add(phoneUser)
            phoneUserSet.add(
                hashString(
                    formatE164Number(requireContext(), countryCode, phoneNumber).toString()
                )
            )
            Timber.d("Adding phone user: ${phoneUser.name} ${phoneUser.number}")
        }
        DatabaseUtils.dumpCursor(phones)

        viewModel.writePhoneUsers(phoneUsers)
        viewModel.writeContactsToSharedPref(phoneUserSet.toList())
    }

    private fun checkContactsPermission() {
        contactsPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    fetchAllUserContacts()
                } else {
                    Timber.d("Couldn't send contacts. No permission granted.")
                }
            }

        when {
            context?.let {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.READ_CONTACTS
                )
            } == PackageManager.PERMISSION_GRANTED -> {
                if (!viewModel.areUsersFetched()) {
                    fetchAllUserContacts()
                }
            }

            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                // TODO show why permission is needed
            }

            else -> contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }
}