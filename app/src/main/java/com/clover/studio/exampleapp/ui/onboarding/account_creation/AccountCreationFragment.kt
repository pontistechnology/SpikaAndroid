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
import com.clover.studio.exampleapp.utils.Tools.convertBitmapToUri
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*


const val CHUNK_SIZE = 32000

class AccountCreationFragment : BaseFragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var sha256FileHash: String? = ""

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it)
                val bitmapUri = convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).into(binding.ivPickPhoto)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
                currentPhotoLocation = bitmapUri
                calculateSha256Hash()
            } else {
                Timber.d("Gallery error")
            }
        }

    private val takePhotoContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), currentPhotoLocation)
                val bitmapUri = convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).into(binding.ivPickPhoto)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
                currentPhotoLocation = bitmapUri
                calculateSha256Hash()
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

        viewModel.uploadStateListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                OnboardingStates.UPLOAD_SUCCESS -> viewModel.updateUserData(hashMapOf(Const.UserData.DISPLAY_NAME to binding.etEnterUsername.text.toString()))
                OnboardingStates.UPLOAD_ERROR -> binding.clProgressScreen.visibility = View.GONE
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
            if (currentPhotoLocation != Uri.EMPTY) {
                val inputStream =
                    requireActivity().contentResolver.openInputStream(currentPhotoLocation)
                Timber.d("File upload start")
                uploadFile(Tools.copyStreamToFile(requireActivity(), inputStream!!))
                binding.clProgressScreen.visibility = View.VISIBLE
            }
        }
    }

    private fun calculateSha256Hash() {
        val inputStream =
            requireActivity().contentResolver.openInputStream(currentPhotoLocation)
        sha256FileHash =
            Tools.calculateSHA256FileHash(Tools.copyStreamToFile(requireActivity(), inputStream!!))
    }

    private fun choosePhoto() {
        chooseImageContract.launch(Const.JsonFields.IMAGE)
    }

    private fun takePhoto() {
        currentPhotoLocation = FileProvider.getUriForFile(
            requireActivity(),
            "com.clover.studio.exampleapp.fileprovider",
            Tools.createImageFile(requireActivity())
        )
        Timber.d("$currentPhotoLocation")
        takePhotoContract.launch(currentPhotoLocation)
    }

    private fun uploadFile(file: File) {
        Timber.d("${file.length()}")
        val pieces: Long =
            if ((file.length() % CHUNK_SIZE).toInt() != 0)
                file.length() / CHUNK_SIZE + 1
            else file.length() / CHUNK_SIZE

        BufferedInputStream(FileInputStream(file)).use { bis ->
            var len: Int
            var piece = 0L
            val temp = ByteArray(CHUNK_SIZE)
            val randomId = UUID.randomUUID().toString().substring(0, 7)
            while (bis.read(temp).also { len = it } > 0) {
                val fileChunk = FileChunk(
                    Base64.encodeToString(
                        temp,
                        0,
                        len,
                        0
                    ),
                    piece,
                    pieces,
                    file.length(),
                    Const.JsonFields.IMAGE,
                    file.name.toString(),
                    randomId,
                    sha256FileHash,
                    Const.JsonFields.AVATAR,
                    1
                )

                viewModel.uploadFile(fileChunk.chunkToJson(), pieces)

                Timber.d("$fileChunk")
                piece++
            }
        }
    }
}