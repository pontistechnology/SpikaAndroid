package com.clover.studio.exampleapp.ui.main.settings

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.MessageBody
import com.clover.studio.exampleapp.databinding.FragmentSettingsBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.utils.*
import com.clover.studio.exampleapp.utils.Tools.getFilePathUrl
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class SettingsFragment : BaseFragment() {
    // TODO move this to viewModel
    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    private val viewModel: MainViewModel by activityViewModels()
    private var bindingSetup: FragmentSettingsBinding? = null
    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var progress: Long = 1L
    private var avatarId: Long? = 0L

    private val binding get() = bindingSetup!!

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it, false)
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
                    Tools.handleSamplingAndRotationBitmap(
                        requireActivity(),
                        currentPhotoLocation,
                        false
                    )
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
        initializeViews()
        addTextListeners()

        // Display version code on bottom of the screen
        val packageInfo =
            requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
        // Bug fix for older devices
        binding.tvVersionNumber.text =
            "${getString(R.string.app_version)} ${packageInfo.versionName} ${
                PackageInfoCompat.getLongVersionCode(
                    packageInfo
                )
            }"

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
                    hideKeyboard(view)
                }
            }
        }
    }

    private fun initializeObservers() {
        viewModel.getLocalUser().observe(viewLifecycleOwner) {
            val response = it.responseData
            if (response != null) {
                binding.tvUsername.text = response.displayName ?: getString(R.string.no_username)
                binding.tvPhoneNumber.text = response.telephoneNumber
                avatarId = response.avatarFileId

                Glide.with(requireActivity())
                    .load(response.avatarFileId?.let { fileId -> getFilePathUrl(fileId) })
                    .placeholder(R.drawable.img_user_placeholder)
                    .centerCrop()
                    .into(binding.ivPickPhoto)
            }
        }

        /*viewModel.userUpdateListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                UserUpdated -> {
                    showUserDetails()
                }
                UserUpdateFailed -> Timber.d("User update failed")
                else -> Timber.d("Other error")
            }
        })*/
    }

    private fun showUserDetails() {
        binding.tvUsername.visibility = View.VISIBLE
        binding.tvPhoneNumber.visibility = View.VISIBLE
        binding.etEnterUsername.visibility = View.INVISIBLE
        binding.tvDone.visibility = View.GONE
    }

    private fun initializeViews() {
        when (viewModel.getUserTheme()) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.tvActiveTheme.text =
                getString(R.string.light_theme)
            AppCompatDelegate.MODE_NIGHT_YES -> binding.tvActiveTheme.text =
                getString(R.string.dark_theme)
            else -> binding.tvActiveTheme.text = getString(R.string.system_theme)
        }
    }

    private fun setupClickListeners() {

        // Removed and waiting for each respective screen to be implemented
        binding.clPrivacy.setOnClickListener {
            goToPrivacySettings()
        }
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

        binding.clAppearance.setOnClickListener {
            goToAppearanceSettings()
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
                if ((fileStream.length() % getChunkSize(fileStream.length())).toInt() != 0)
                    (fileStream.length() / getChunkSize(fileStream.length()) + 1).toInt()
                else (fileStream.length() / getChunkSize(fileStream.length())).toInt()

            binding.progressBar.max = uploadPieces
            Timber.d("File upload start")
            CoroutineScope(Dispatchers.IO).launch {
                uploadDownloadManager.uploadFile(
                    requireActivity(),
                    currentPhotoLocation,
                    Const.JsonFields.AVATAR_TYPE,
                    uploadPieces,
                    fileStream,
                    null,
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
                            fileId: Long,
                            fileType: String,
                            messageBody: MessageBody?
                        ) {
                            Timber.d("Upload verified")
                            requireActivity().runOnUiThread {
                                binding.clProgressScreen.visibility = View.GONE
                            }

                            val jsonObject = JsonObject()
                            jsonObject.addProperty(Const.UserData.AVATAR_FILE_ID, fileId)
//                            val userData = hashMapOf(
//                                Const.UserData.AVATAR_FILE_ID to fileId
//                            )
                            viewModel.updateUserData(jsonObject)
                        }

                    })
            }
            binding.clProgressScreen.visibility = View.VISIBLE
        }
    }

    private fun updateUsername() {
        if (binding.etEnterUsername.text.toString().isNotEmpty()) {
            val jsonObject = JsonObject()
            jsonObject.addProperty(
                Const.UserData.DISPLAY_NAME,
                binding.etEnterUsername.text.toString()
            )
            jsonObject.addProperty(
                Const.JsonFields.AVATAR_FILE_ID,
                avatarId
            )
            viewModel.updateUserData(jsonObject)
        }
        binding.etEnterUsername.visibility = View.GONE
        binding.tvUsername.visibility = View.VISIBLE
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
            BuildConfig.APPLICATION_ID + ".fileprovider",
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

    private fun goToAppearanceSettings() {
        findNavController().navigate(R.id.action_mainFragment_to_appearanceSettings)
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