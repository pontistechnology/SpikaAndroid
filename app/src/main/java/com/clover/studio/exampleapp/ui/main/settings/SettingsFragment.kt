package com.clover.studio.exampleapp.ui.main.settings

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.FragmentSettingsBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.ui.main.UserUpdateFailed
import com.clover.studio.exampleapp.ui.main.UserUpdated
import com.clover.studio.exampleapp.utils.*
import com.clover.studio.exampleapp.utils.Tools.getAvatarUrl
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment() {
    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    private val viewModel: MainViewModel by activityViewModels()
    private var bindingSetup: FragmentSettingsBinding? = null
    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var progress: Long = 1L

    private val binding get() = bindingSetup!!

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).into(binding.ivPickPhoto)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
                currentPhotoLocation = bitmapUri
                updateUserImage()
            } else {
                Timber.d("Gallery error")
            }
        }

    private val takePhotoContract =
        registerForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), currentPhotoLocation)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).into(binding.ivPickPhoto)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
                currentPhotoLocation = bitmapUri
                updateUserImage()
            } else {
                Timber.d("Photo error")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentSettingsBinding.inflate(inflater, container, false)

        setupClickListeners()
        initializeObservers()
        addTextListeners()

        // Display version code on bottom of the screen
        val packageInfo =
            requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
        binding.tvVersionNumber.text =
            "${getString(R.string.app_version)} ${packageInfo.versionName} ${packageInfo.longVersionCode}"

        return binding.root
    }

    private fun addTextListeners() {
        binding.etEnterUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // ignore
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!text.isNullOrEmpty()) {
                    binding.tvDone.isEnabled = true
                }
            }

            override fun afterTextChanged(text: Editable?) {
                if (!text.isNullOrEmpty()) {
                    binding.tvDone.isEnabled = true
                }
            }
        })

        binding.etEnterUsername.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus) {
                    Tools.hideKeyboard(requireActivity(), view)
                }
            }
        }
    }

    private fun initializeObservers() {
        viewModel.getLocalUser().observe(viewLifecycleOwner) {
            binding.tvUsername.text = it.displayName ?: getString(R.string.no_username)
            binding.tvPhoneNumber.text = it.telephoneNumber

            Glide.with(requireActivity())
                .load(it.avatarFileId?.let { fileId -> getAvatarUrl(fileId) })
                .placeholder(context?.getDrawable(R.drawable.img_user_placeholder))
                .into(binding.ivPickPhoto)
        }

        viewModel.userUpdateListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                UserUpdated -> {
                    showUserDetails()
                }
                UserUpdateFailed -> Timber.d("User update failed")
                else -> Timber.d("Other error")
            }
        })
    }

    private fun showUserDetails() {
        binding.tvUsername.visibility = View.VISIBLE
        binding.tvPhoneNumber.visibility = View.VISIBLE
        binding.etEnterUsername.visibility = View.INVISIBLE
        binding.tvDone.visibility = View.GONE
    }

    private fun setupClickListeners() {

        // Removed and waiting for each respective screen to be implemented
//        binding.clPrivacy.setOnClickListener {
//            goToPrivacySettings()
//        }
//
//        binding.clChat.setOnClickListener {
//            goToChatSettings()
//        }
//
//        binding.clNotifications.setOnClickListener {
//            goToNotificationSettings()
//        }
//
//        binding.clMediaDownload.setOnClickListener {
//            goToDownloadSettings()
//        }

        binding.ivPickPhoto.setOnClickListener {
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

        binding.tvUsername.setOnClickListener {
            showUsernameUpdate()
        }

        binding.tvPhoneNumber.setOnClickListener {
            showUsernameUpdate()
        }

        binding.tvDone.setOnClickListener {
            updateUsername()
        }
    }

    private fun updateUserImage() {
        if (currentPhotoLocation != Uri.EMPTY) {
            val inputStream =
                requireActivity().contentResolver.openInputStream(currentPhotoLocation)

            val fileStream = Tools.copyStreamToFile(
                requireActivity(),
                inputStream!!,
                activity?.contentResolver?.getType(currentPhotoLocation)!!
            )
            val uploadPieces =
                if ((fileStream.length() % CHUNK_SIZE).toInt() != 0)
                    fileStream.length() / CHUNK_SIZE + 1
                else fileStream.length() / CHUNK_SIZE

            binding.progressBar.max = uploadPieces.toInt()
            Timber.d("File upload start")
            CoroutineScope(Dispatchers.IO).launch {
                uploadDownloadManager.uploadFile(
                    requireActivity(),
                    currentPhotoLocation,
                    Const.JsonFields.AVATAR_TYPE,
                    uploadPieces,
                    fileStream,
                    false,
                    object :
                        FileUploadListener {
                        override fun filePieceUploaded() {
                            if (progress <= uploadPieces) {
                                binding.progressBar.secondaryProgress = progress.toInt()
                                progress++
                            } else progress = 0
                        }

                        override fun fileUploadError(description: String) {
                            Timber.d("Upload Error")
                            requireActivity().runOnUiThread {
                                showUploadError(description)
                            }
                        }

                        override fun fileUploadVerified(
                            path: String,
                            mimeType: String,
                            thumbId: Long,
                            fileId: Long
                        ) {
                            Timber.d("Upload verified")
                            requireActivity().runOnUiThread {
                                binding.clProgressScreen.visibility = View.GONE
                            }

                            val userData = hashMapOf(
                                Const.UserData.AVATAR_URL to path
                            )
                            viewModel.updateUserData(userData)
                        }

                    })
            }
            binding.clProgressScreen.visibility = View.VISIBLE
        }
    }

    private fun updateUsername() {
        if (binding.etEnterUsername.text.toString().isNotEmpty()) {
            viewModel.updateUserData(hashMapOf(Const.UserData.DISPLAY_NAME to binding.etEnterUsername.text.toString()))
        } else {
            return
        }
    }

    private fun showUsernameUpdate() {
        binding.tvUsername.visibility = View.INVISIBLE
        binding.tvPhoneNumber.visibility = View.INVISIBLE
        binding.etEnterUsername.visibility = View.VISIBLE
        binding.tvDone.visibility = View.VISIBLE
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

    private fun showUploadError(description: String) {
        DialogError.getInstance(requireActivity(),
            getString(R.string.error),
            getString(R.string.image_failed_upload, description),
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

    // Below navigation methods are unused until we implement all other functionality of settings
    // screens
    private fun goToPrivacySettings() {
        findNavController().navigate(R.id.action_mainFragment_to_privacySettingsFragment22)
    }

    private fun goToChatSettings() {
        findNavController().navigate(R.id.action_mainFragment_to_chatSettingsFragment22)
    }

    private fun goToNotificationSettings() {
        findNavController().navigate(R.id.action_mainFragment_to_notificationSettingsFragment22)
    }

    private fun goToDownloadSettings() {
        findNavController().navigate(R.id.action_mainFragment_to_downloadSettingsFragment2)
    }

    override fun onPause() {
        super.onPause()
        showUserDetails()
    }
}