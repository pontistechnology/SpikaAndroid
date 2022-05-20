package com.clover.studio.exampleapp.ui.onboarding.account_creation

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.UploadFile
import com.clover.studio.exampleapp.databinding.FragmentAccountCreationBinding
import com.clover.studio.exampleapp.ui.main.startMainActivity
import com.clover.studio.exampleapp.ui.onboarding.*
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.Tools.convertBitmapToUri
import com.clover.studio.exampleapp.utils.Tools.sha256HashFromUri
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.dialog.DialogInteraction
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*


const val CHUNK_SIZE = 64000

class AccountCreationFragment : BaseFragment() {
    private val viewModel: OnboardingViewModel by activityViewModels()
    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var uploadPieces: Long = 0L
    private var progress: Long = 1L
    private var uploadFile: UploadFile? = null

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it)
                val bitmapUri = convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).into(binding.ivPickPhoto)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
                currentPhotoLocation = bitmapUri
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
                OnboardingStates.USER_UPDATED -> {
                    startMainActivity(requireActivity())
                }
                OnboardingStates.USER_UPDATE_ERROR -> Timber.d("Error updating user")
                else -> Timber.d("Other error")
            }
        })

        viewModel.uploadStateListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                UploadPiece -> {
                    Timber.d("PROGRESS $progress, $uploadPieces, ${binding.progressBar.max}")
                    if (progress <= uploadPieces) {
                        binding.progressBar.secondaryProgress = progress.toInt()
                        progress++
                    } else progress = 0
                }
                UploadSuccess -> viewModel.verifyUploadedFile(uploadFile!!.fileToJson())
                is UploadVerified -> {
                    binding.progressBar.secondaryProgress = uploadPieces.toInt()
                    val userData = hashMapOf(
                        Const.UserData.DISPLAY_NAME to binding.etEnterUsername.text.toString(),
                        Const.UserData.AVATAR_URL to it.path
                    )
                    viewModel.updateUserData(userData)
                }
                UploadVerificationFailed -> showUploadError()
                UploadError -> showUploadError()
            }
        })
    }

    private fun addClickListeners() {
        binding.btnNext.setOnClickListener {
            activity?.currentFocus?.let { view ->
                val imm =
                    requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
            checkUsername()
        }

        binding.cvPhotoPicker.setOnClickListener {
            ChooserDialog.getInstance(requireContext(),
                getString(R.string.placeholder_title),
                null,
                getString(R.string.choose_from_gallery),
                getString(R.string.take_photo),
                object : DialogInteraction {
                    override fun onFirstOptionClicked() {
                        chooseImage()
                    }

                    override fun onSecondOptionClicked() {
                        takePhoto()
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

    private fun showUploadError() {
        DialogError.getInstance(requireActivity(),
            getString(R.string.error),
            getString(R.string.image_failed_upload),
            null,
            getString(R.string.ok),
            object : DialogInteraction {
                override fun onFirstOptionClicked() {
                    // ignore
                }

                override fun onSecondOptionClicked() {
                    // ignore
                }
            })
        binding.clProgressScreen.visibility = View.GONE
        binding.progressBar.secondaryProgress = 0
        currentPhotoLocation = Uri.EMPTY
        Glide.with(this).clear(binding.ivPickPhoto)
        binding.ivPickPhoto.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.img_camera
            )
        )
        binding.clSmallCameraPicker.visibility = View.GONE
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
            } else {
                viewModel.updateUserData(hashMapOf(Const.UserData.DISPLAY_NAME to binding.etEnterUsername.text.toString()))
            }
        }
    }

    private fun chooseImage() {
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
        uploadPieces =
            if ((file.length() % CHUNK_SIZE).toInt() != 0)
                file.length() / CHUNK_SIZE + 1
            else file.length() / CHUNK_SIZE

        binding.progressBar.max = uploadPieces.toInt()

        BufferedInputStream(FileInputStream(file)).use { bis ->
            var len: Int
            var piece = 0L
            val temp = ByteArray(CHUNK_SIZE)
            val randomId = UUID.randomUUID().toString().substring(0, 7)
            while (bis.read(temp).also { len = it } > 0) {
                uploadFile = UploadFile(
                    Base64.encodeToString(
                        temp,
                        0,
                        len,
                        0
                    ),
                    piece,
                    uploadPieces,
                    file.length(),
                    Const.JsonFields.IMAGE,
                    file.name.toString(),
                    randomId,
                    sha256HashFromUri(requireActivity(), currentPhotoLocation),
                    Const.JsonFields.AVATAR,
                    1
                )

                viewModel.uploadFile(uploadFile!!.chunkToJson(), uploadPieces)

                Timber.d("$uploadFile")
                piece++
            }
        }
    }
}