package com.clover.studio.spikamessenger.ui.main.settings

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
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.BuildConfig
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.databinding.FragmentSettingsBinding
import com.clover.studio.spikamessenger.ui.main.MainFragmentDirections
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.FileUploadListener
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.Tools.getFilePathUrl
import com.clover.studio.spikamessenger.utils.UploadDownloadManager
import com.clover.studio.spikamessenger.utils.dialog.ChooserDialog
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.getChunkSize
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

    private var navOptionsBuilder: NavOptions? = null

    private val binding get() = bindingSetup!!

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it, false)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).into(binding.profilePicture.ivPickAvatar)
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

                Glide.with(this).load(bitmap).into(binding.profilePicture.ivPickAvatar)
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

        navOptionsBuilder = Tools.createCustomNavOptions()

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
                // Ignore
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!text.isNullOrEmpty()) {
                    binding.ivDone.isEnabled = true
                }
            }

            override fun afterTextChanged(text: Editable?) {
                if (!text.isNullOrEmpty()) {
                    binding.ivDone.isEnabled = true
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

    private fun initializeObservers() = with(binding){
        viewModel.getLocalUser().observe(viewLifecycleOwner) {
            val response = it.responseData
            if (response != null) {
                tvUsername.text = response.formattedDisplayName
                tvPhoneNumber.text = response.telephoneNumber
                avatarId = response.avatarFileId

                Glide.with(requireActivity())
                    .load(response.avatarFileId?.let { fileId -> getFilePathUrl(fileId) })
                    .placeholder(R.drawable.img_user_placeholder)
                    .centerCrop()
                    .into(profilePicture.ivPickAvatar)
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

    private fun showUserDetails() = with(binding){
        tvUsername.visibility = View.VISIBLE
        tvPhoneNumber.visibility = View.VISIBLE
        etEnterUsername.visibility = View.INVISIBLE
        ivDone.visibility = View.GONE
    }

    private fun initializeViews() = with(binding) {
        when (viewModel.getUserTheme()) {
            Const.Themes.NEON_THEME -> tvActiveTheme.text = "Neon"
            Const.Themes.MINT_THEME -> tvActiveTheme.text = "Mint"
            else -> binding.tvActiveTheme.text = "Base"
        }
    }

    private fun setupClickListeners() = with(binding) {

        // Removed and waiting for each respective screen to be implemented
        flPrivacy.setOnClickListener {
            goToPrivacySettings()
        }

        profilePicture.ivPickAvatar.setOnClickListener {
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

        tvUsername.setOnClickListener {
            showUsernameUpdate()
        }

        tvPhoneNumber.setOnClickListener {
            showUsernameUpdate()
        }

        ivDone.setOnClickListener {
            updateUsername()
        }

        clAppearance.setOnClickListener {
            goToAppearanceSettings()
        }

        clDeleteUser.setOnClickListener {
            DialogError.getInstance(requireContext(),
                getString(R.string.warning),
                getString(R.string.data_deletion_warning),
                getString(R.string.cancel),
                getString(R.string.ok),
                object : DialogInteraction {
                    override fun onFirstOptionClicked() {
                        // Ignore
                    }

                    override fun onSecondOptionClicked() {
                        viewModel.deleteUser()
                    }
                })
        }
    }

    private fun updateUserImage() {
        if (currentPhotoLocation != Uri.EMPTY) {
            val inputStream =
                requireActivity().contentResolver.openInputStream(currentPhotoLocation)

            val fileStream = Tools.copyStreamToFile(
                inputStream!!,
                activity?.contentResolver?.getType(currentPhotoLocation)!!
            )
            val uploadPieces =
                if ((fileStream.length() % getChunkSize(fileStream.length())).toInt() != 0)
                    (fileStream.length() / getChunkSize(fileStream.length()) + 1).toInt()
                else (fileStream.length() / getChunkSize(fileStream.length())).toInt()

            binding.profilePicture.progressBar.max = uploadPieces
            Timber.d("File upload start")
            CoroutineScope(Dispatchers.IO).launch {
                uploadDownloadManager.uploadFile(
                    FileData(
                        currentPhotoLocation,
                        Const.JsonFields.AVATAR_TYPE,
                        uploadPieces,
                        fileStream,
                        null,
                        false,
                        null,
                        0,
                        null,
                        null
                    ),
                    object :
                        FileUploadListener {
                        override fun filePieceUploaded() {
                            if (progress <= uploadPieces) {
                                binding.profilePicture.progressBar.secondaryProgress = progress.toInt()
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
                                binding.profilePicture.flProgressScreen.visibility = View.GONE
                            }

                            val jsonObject = JsonObject()
                            jsonObject.addProperty(Const.UserData.AVATAR_FILE_ID, fileId)
                            viewModel.updateUserData(jsonObject)
                        }

                        override fun fileCanceledListener(messageId: String?) {
                            // Ignore
                        }

                    })
            }
            binding.profilePicture.flProgressScreen.visibility = View.VISIBLE
        }
    }

    private fun updateUsername() = with(binding) {
        if (etEnterUsername.text.toString().isNotEmpty()) {
            val jsonObject = JsonObject()
            jsonObject.addProperty(
                Const.UserData.DISPLAY_NAME,
                etEnterUsername.text.toString()
            )
            jsonObject.addProperty(
                Const.JsonFields.AVATAR_FILE_ID,
                avatarId
            )
            viewModel.updateUserData(jsonObject)
        }
        etEnterUsername.visibility = View.GONE
        tvUsername.visibility = View.VISIBLE
        ivDone.visibility = View.GONE
    }

    private fun showUsernameUpdate() = with(binding) {
        tvUsername.visibility = View.INVISIBLE
        tvPhoneNumber.visibility = View.INVISIBLE
        etEnterUsername.visibility = View.VISIBLE
        ivDone.visibility = View.VISIBLE
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

    private fun showUploadError(description: String) = with(binding) {
        DialogError.getInstance(requireActivity(),
            getString(R.string.error),
            getString(R.string.image_failed_upload, description),
            null,
            getString(R.string.ok),
            object : DialogInteraction {
                override fun onFirstOptionClicked() {
                    // Ignore
                }

                override fun onSecondOptionClicked() {
                    // Ignore
                }
            })
        profilePicture.flProgressScreen.visibility = View.GONE
        profilePicture.progressBar.secondaryProgress = 0
        currentPhotoLocation = Uri.EMPTY
        Glide.with(this@SettingsFragment).clear(profilePicture.ivPickAvatar)
        profilePicture.ivPickAvatar.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.img_camera
            )
        )
    }

    // Below navigation methods are unused until we implement all other functionality of settings
    // screens
    private fun goToPrivacySettings() {
        findNavController().navigate(
            MainFragmentDirections.actionMainFragmentToPrivacySettingsFragment(),
            navOptionsBuilder
        )
    }

    private fun goToAppearanceSettings() {
        findNavController().navigate(
            MainFragmentDirections.actionMainFragmentToAppearanceSettings(),
            navOptionsBuilder
        )
    }

    override fun onPause() {
        showUserDetails()
        super.onPause()
    }
}
