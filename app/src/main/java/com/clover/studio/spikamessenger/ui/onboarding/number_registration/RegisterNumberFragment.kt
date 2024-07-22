package com.clover.studio.spikamessenger.ui.onboarding.number_registration

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.FragmentRegisterNumberBinding
import com.clover.studio.spikamessenger.ui.onboarding.OnboardingViewModel
import com.clover.studio.spikamessenger.utils.AppPermissions
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.Tools.hashString
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.gson.JsonObject
import timber.log.Timber

class RegisterNumberFragment : BaseFragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var countryCode: String
    private lateinit var multiplePermissionLauncher: ActivityResultLauncher<Array<String>>

    private var bindingSetup: FragmentRegisterNumberBinding? = null

    private var phoneNumber: String = ""
    private var deviceId: String? = null
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
        bindingSetup = FragmentRegisterNumberBinding.inflate(inflater, container, false)

        checkUser()
        checkMultiplePermissions()
        setTextListener()
        setClickListeners()
        setObservers()

        viewModel.readToken()

        return binding.root
    }

    private fun checkUser() {
        if (viewModel.isAppStarted()) {
            phoneNumber = viewModel.readPhoneNumber()
            countryCode = viewModel.readCountryCode()
            deviceId = viewModel.readDeviceId()

            Timber.d("Device id = $deviceId")
            if (deviceId?.isNotEmpty() == true) {
                binding.etPhoneNumber.visibility = View.GONE
                binding.tvDefaultPhoneNumber.visibility = View.VISIBLE
                binding.tvDefaultPhoneNumber.text = phoneNumber
                binding.btnNext.isEnabled = true
            }
        }
        binding.tvCountryCode.text = countryCode
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingSetup = null
    }

    private fun setObservers() {
        viewModel.registrationListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    val bundle = bundleOf(
                        Const.Navigation.PHONE_NUMBER to countryCode + phoneNumber.trimStart('0'),
                        Const.Navigation.PHONE_NUMBER_HASHED to hashString(
                            countryCode + phoneNumber.trimStart('0')
                        ),
                        Const.Navigation.COUNTRY_CODE to countryCode.substring(1),
                        Const.Navigation.DEVICE_ID to deviceId
                    )
                    viewModel.registerFlag(true)
                    viewModel.writeDeviceId(deviceId.toString())
                    viewModel.writeFirstAppStart()
                    findNavController().navigate(
                        R.id.action_registerNumberFragment_to_verificationFragment,
                        bundle
                    )
                }

                Resource.Status.LOADING -> {
                    binding.btnNext.isEnabled = false
                }

                Resource.Status.ERROR -> {
                    DialogError.getInstance(requireContext(),
                        getString(R.string.registration_error),
                        "${getString(R.string.registration_error_description)} \n\n ${it.message}",
                        null, getString(R.string.ok), object : DialogInteraction {
                            override fun onFirstOptionClicked() {
                                // Ignore
                            }

                            override fun onSecondOptionClicked() {
                                // Ignore
                            }

                        })
                    binding.btnNext.isEnabled = true
                }

                else -> Timber.d("Other error")
            }
        })

        viewModel.userPhoneNumberListener.observe(viewLifecycleOwner) {
            binding.etPhoneNumber.setText(it)
        }
    }

    private fun setClickListeners() {
        if (!viewModel.isAppStarted()) {
            binding.tvCountryCode.setOnClickListener {
                if (binding.etPhoneNumber.text.isNotEmpty()) {
                    viewModel.userPhoneNumberListener.value = binding.etPhoneNumber.text.toString()
                }
                findNavController().navigate(
                    RegisterNumberFragmentDirections.actionRegisterNumberFragmentToCountryPickerFragment()
                )
            }
        }

        binding.btnNext.setOnClickListener {
            if (phoneNumber.isEmpty()) {
                phoneNumber = binding.etPhoneNumber.text.toString()
                countryCode = binding.tvCountryCode.text.toString()
                viewModel.writePhoneAndCountry(phoneNumber, countryCode)
            }
            viewModel.sendNewUserData(getJsonObject())
            binding.btnNext.isEnabled = false
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

        binding.etPhoneNumber.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
        }
    }

    private fun getJsonObject(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty(
            Const.JsonFields.TELEPHONE_NUMBER,
            countryCode + phoneNumber.trimStart('0')
        )
        jsonObject.addProperty(
            Const.JsonFields.TELEPHONE_NUMBER_HASHED, hashString(
                countryCode + phoneNumber.trimStart('0')
            )
        )
        jsonObject.addProperty(Const.JsonFields.COUNTRY_CODE, countryCode.substring(1))

        Timber.d("Device id = $deviceId")
        if (deviceId == null || deviceId?.isEmpty() == true) {
            deviceId = Tools.generateRandomId()
        }
        jsonObject.addProperty(Const.JsonFields.DEVICE_ID, deviceId)

        return jsonObject
    }

    @SuppressLint("Range")
    private fun fetchAllUserContacts() {
        val contacts = Tools.fetchPhonebookContacts(requireContext(), countryCode)

        if (contacts != null) {
            viewModel.writePhoneUsers(contacts)
            viewModel.writeContactsToSharedPref(
                Tools.getContactsNumbersHashed(
                    requireContext(),
                    countryCode,
                    contacts
                ).toList()
            )
        }
    }

    private fun checkMultiplePermissions() {
        val permissionsToRequest = AppPermissions.requestPermissions(requireActivity())

        multiplePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsMap ->
                if (permissionsMap.isNotEmpty()) {
                    if (permissionsMap[Manifest.permission.READ_CONTACTS] == true) {
                        if (!viewModel.areUsersFetched()) {
                            fetchAllUserContacts()
                        }
                    } else {
                        Timber.d("Couldn't fetch contacts or access storage or post notifications. Permissions not granted.")
                    }
                }
            }

        if (permissionsToRequest.isNotEmpty()) {
            multiplePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            fetchAllUserContacts()
        }
    }
}
