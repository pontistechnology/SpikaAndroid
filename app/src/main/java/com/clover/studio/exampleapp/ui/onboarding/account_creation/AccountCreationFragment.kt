package com.clover.studio.exampleapp.ui.onboarding.account_creation

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.data.models.networking.FileChunk
import com.clover.studio.exampleapp.databinding.FragmentAccountCreationBinding
import com.clover.studio.exampleapp.ui.main.startMainActivity
import com.clover.studio.exampleapp.ui.onboarding.OnboardingStates
import com.clover.studio.exampleapp.ui.onboarding.OnboardingViewModel
import com.clover.studio.exampleapp.utils.ChooserDialog
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*


const val chunkSize = 32000

class AccountCreationFragment : Fragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var currentPhotoLocation: Uri
    private var md5FileHash: String? = ""

    private val choosePhotoContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                Glide.with(this).load(it).into(binding.ivPickPhoto)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
                currentPhotoLocation = it
                calculateMd5Hash()
            } else {
                Timber.d("Gallery error")
            }
        }

    private val takePhotoContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                Glide.with(this).load(currentPhotoLocation).into(binding.ivPickPhoto)
                calculateMd5Hash()
            } else {
                Timber.d("Photo error")
            }
        }

    private var bindingSetup: FragmentAccountCreationBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        bindingSetup = FragmentAccountCreationBinding.inflate(inflater, container, false)

        addTextListeners()
        addClickListeners()
        addObservers()

        viewModel.sendContacts()
        return binding.root
    }

    private fun addObservers() {
        viewModel.accountCreationListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                OnboardingStates.CONTACTS_SENT -> Timber.d("Contacts sent successfully")
                OnboardingStates.CONTACTS_ERROR -> Timber.d("Failed to send contacts")
                else -> Timber.d("Other error")
            }
        })

        viewModel.userUpdateListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                OnboardingStates.USER_UPDATED -> startMainActivity(requireActivity())
                OnboardingStates.USER_UPDATE_ERROR -> Timber.d("Error updating user")
                else -> Timber.d("Other error")
            }
        })
    }

    private fun addClickListeners() {
        binding.btnNext.setOnClickListener {
            checkUsername()
        }

        binding.cvPhotoPicker.setOnClickListener {
            ChooserDialog.getInstance(requireContext(), object : ChooserDialog.DialogInteraction {
                override fun onPhotoClicked() {
                    takePhoto()
                }

                override fun onGalleryClicked() {
                    choosePhoto()
                }
            })
        }
    }

    private fun addTextListeners() {
        binding.etEnterUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // ignore
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!text.isNullOrEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.clUsernameError.visibility = View.INVISIBLE
                }
            }

            override fun afterTextChanged(text: Editable?) {
                if (!text.isNullOrEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.clUsernameError.visibility = View.INVISIBLE
                }
            }
        })
    }

    private fun checkUsername() {
        if (binding.etEnterUsername.text.isNullOrEmpty()) {
            binding.btnNext.isEnabled = false
            binding.clUsernameError.visibility = View.VISIBLE
        } else {
            val inputStream =
                requireActivity().contentResolver.openInputStream(currentPhotoLocation)
            uploadFile(Tools.copyStreamToFile(requireActivity(), inputStream!!))
            viewModel.updateUserData(hashMapOf(Const.UserData.DISPLAY_NAME to binding.etEnterUsername.text.toString()))
        }
    }

    private fun calculateMd5Hash() {
        val inputStream =
            requireActivity().contentResolver.openInputStream(currentPhotoLocation)
        md5FileHash =
            Tools.calculateMD5(Tools.copyStreamToFile(requireActivity(), inputStream!!))
    }

    private fun choosePhoto() {
        choosePhotoContract.launch(Const.JsonFields.IMAGE)
    }

    private fun takePhoto() {
        currentPhotoLocation = FileProvider.getUriForFile(
            requireActivity(),
            "com.clover.studio.exampleapp.fileprovider",
            Tools.createImageFile(requireActivity())
        )
        takePhotoContract.launch(currentPhotoLocation)
    }

    private fun uploadFile(file: File) {
        Timber.d("${file.length()}")
        val pieces = file.length() / chunkSize

        val stream = BufferedInputStream(FileInputStream(file))
        val buffer = ByteArray(chunkSize)
        val randomId = UUID.randomUUID().toString().substring(0, 7)

        for (piece in 0 until pieces) {
            if (stream.read(buffer) == -1) break

            val base64 = Base64.encodeToString(buffer, Base64.DEFAULT)

            val fileChunk = FileChunk(
                base64,
                piece,
                pieces,
                file.length(),
                Const.JsonFields.IMAGE,
                file.name.toString(),
                randomId,
                md5FileHash,
                Const.JsonFields.AVATAR,
                1
            )

            viewModel.uploadFile(fileChunk.chunkToJson())
        }
    }
}