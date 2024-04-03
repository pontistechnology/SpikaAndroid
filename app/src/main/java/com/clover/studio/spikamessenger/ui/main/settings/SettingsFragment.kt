package com.clover.studio.spikamessenger.ui.main.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clover.studio.spikamessenger.BuildConfig
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.databinding.FragmentSettingsBinding
import com.clover.studio.spikamessenger.ui.main.MainFragmentDirections
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.Tools.getFilePathUrl
import com.clover.studio.spikamessenger.utils.UserOptions
import com.clover.studio.spikamessenger.utils.dialog.ChooserDialog
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.getChunkSize
import com.clover.studio.spikamessenger.utils.helpers.UploadService
import com.clover.studio.spikamessenger.utils.helpers.UserOptionsData
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


@AndroidEntryPoint
class SettingsFragment : BaseFragment(), ServiceConnection {
    private val viewModel: MainViewModel by activityViewModels()
    private var bindingSetup: FragmentSettingsBinding? = null
    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var avatarId: Long? = 0L
    private var uploadPieces: Int = 0
    private var avatarData: FileData? = null

    private var navOptionsBuilder: NavOptions? = null
    private var optionList: MutableList<UserOptionsData> = mutableListOf()

    private val binding get() = bindingSetup!!

    private lateinit var fileUploadService: UploadService
    private var bound = false

    private var progressAnimation: AnimationDrawable? = null
    private var uploadingInProgress = false

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it, false)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.profilePicture.ivPickAvatar)
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
        binding.tvVersionNumber.text = requireContext().getString(
            R.string.app_version,
            packageInfo.versionName.toString(),
            PackageInfoCompat.getLongVersionCode(packageInfo).toString()
        )

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

    private fun initializeObservers() = with(binding) {
        viewModel.getLocalUser().observe(viewLifecycleOwner) {
            val response = it.responseData
            if (response != null) {
                tvUsername.text = response.formattedDisplayName
                tvPhoneNumber.text = response.telephoneNumber
                avatarId = response.avatarFileId

                if (!uploadingInProgress) {
                    response.avatarFileId?.let { fileId ->
                        Glide.with(requireActivity())
                            .load(getFilePathUrl(fileId))
                            .placeholder(R.drawable.img_user_avatar)
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(profilePicture.ivPickAvatar)
                    }
                }
            }
        }
    }

    private fun showUserDetails() = with(binding) {
        tvUsername.visibility = View.VISIBLE
        tvPhoneNumber.visibility = View.VISIBLE
        tilEnterUsername.visibility = View.GONE
        ivDone.visibility = View.GONE
    }

    private fun initializeViews() = with(binding) {
        profilePicture.ivProgressBar.apply {
            setBackgroundResource(R.drawable.drawable_progress_animation)
            progressAnimation = background as AnimationDrawable
        }

        setOptionList()

        val userOptions = UserOptions(requireContext())
        userOptions.setOptions(optionList)
        userOptions.setOptionsListener(object : UserOptions.OptionsListener {
            override fun clickedOption(option: Int, optionName: String) {
                when (optionName) {
                    getString(R.string.appearance) -> {
                        goToAppearanceSettings()
                    }

                    getString(R.string.privacy) -> {
                        goToPrivacySettings()
                    }

                    getString(R.string.delete) -> {
                        deleteAccount()
                    }
                }
            }

            override fun switchOption(optionName: String, isSwitched: Boolean) {
                // Ignore
            }
        })
        flOptionsContainer.addView(userOptions)
    }

    private fun setOptionList() {
        optionList = mutableListOf(
            UserOptionsData(
                option = getString(R.string.appearance),
                firstDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.iv_edit_settings
                ),
                secondDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.img_arrow_forward
                ),
                switchOption = false,
                isSwitched = false
            ),
            UserOptionsData(
                option = getString(R.string.privacy),
                firstDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.iv_privacy
                ),
                secondDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.img_arrow_forward
                ),
                switchOption = false,
                isSwitched = false
            ),
            UserOptionsData(
                option = getString(R.string.delete),
                firstDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.iv_delete_settings
                ),
                secondDrawable = null,
                switchOption = false,
                isSwitched = false
            ),
        )
    }

    private fun deleteAccount() {
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

    private fun setupClickListeners() = with(binding) {
        profilePicture.ivPickAvatar.setOnClickListener {
            val listOptions = mutableListOf(
                getString(R.string.choose_from_gallery) to { chooseImage() },
                getString(R.string.take_photo) to { takePhoto() },
                getString(R.string.cancel) to {}
            )

            ChooserDialog.getInstance(
                context = requireContext(),
                listChooseOptions = listOptions.map { it.first }.toMutableList(),
                object : DialogInteraction {
                    override fun onOptionClicked(optionName: String) {
                        listOptions.find { it.first == optionName }?.second?.invoke()
                    }
                }
            )
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
    }

    private fun updateUserImage() {
        if (currentPhotoLocation != Uri.EMPTY) {
            val inputStream =
                requireActivity().contentResolver.openInputStream(currentPhotoLocation)

            val fileStream = Tools.copyStreamToFile(
                inputStream!!,
                activity?.contentResolver?.getType(currentPhotoLocation)!!
            )

            uploadPieces =
                if ((fileStream.length() % getChunkSize(fileStream.length())).toInt() != 0)
                    (fileStream.length() / getChunkSize(fileStream.length()) + 1).toInt()
                else (fileStream.length() / getChunkSize(fileStream.length())).toInt()


            avatarData = FileData(
                fileUri = currentPhotoLocation,
                fileType = Const.JsonFields.AVATAR_TYPE,
                filePieces = uploadPieces,
                file = fileStream,
                messageBody = null,
                isThumbnail = false,
                localId = null,
                roomId = 0,
                messageStatus = null,
                metadata = null
            )

            binding.profilePicture.flProgressScreen.visibility = View.VISIBLE
            progressAnimation?.start()
            uploadingInProgress = true
            inputStream.close()

            if (bound) {
                CoroutineScope(Dispatchers.Default).launch {
                    avatarData?.let {
                        fileUploadService.uploadAvatar(
                            fileData = it,
                            isGroup = false
                        )
                    }
                }
            } else {
                startUploadService()
            }
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
        tilEnterUsername.visibility = View.GONE
        tvUsername.visibility = View.VISIBLE
        tvPhoneNumber.visibility = View.VISIBLE
        ivDone.visibility = View.GONE
    }

    private fun showUsernameUpdate() = with(binding) {
        tvUsername.visibility = View.GONE
        tvPhoneNumber.visibility = View.GONE
        tilEnterUsername.visibility = View.VISIBLE
        ivDone.visibility = View.VISIBLE

        if (tvUsername.text.isNotEmpty()) {
            etEnterUsername.setText(tvUsername.text)
        }

        showKeyboard(etEnterUsername)
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
        progressAnimation?.stop()
        currentPhotoLocation = Uri.EMPTY
        Glide.with(this@SettingsFragment).clear(profilePicture.ivPickAvatar)
    }


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

    /** Upload service */
    private fun startUploadService() {
        val intent = Intent(MainApplication.appContext, UploadService::class.java)
        MainApplication.appContext.startService(intent)
        activity?.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        showUserDetails()
        uploadingInProgress = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            requireActivity().unbindService(serviceConnection)
        }
        bound = false
    }

    private val serviceConnection = this
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        bound = true

        val binder = service as UploadService.UploadServiceBinder
        fileUploadService = binder.getService()
        fileUploadService.setCallbackListener(object : UploadService.FileUploadCallback {
            override fun uploadError(description: String) {
                Timber.d("Upload Error")
                requireActivity().runOnUiThread {
                    showUploadError(description)
                }
            }

            override fun avatarUploadFinished(fileId: Long) {
                requireActivity().runOnUiThread {
                    binding.profilePicture.flProgressScreen.visibility = View.GONE
                    progressAnimation?.stop()
                }
            }
        })

        CoroutineScope(Dispatchers.Default).launch {
            avatarData?.let { fileUploadService.uploadAvatar(fileData = it, isGroup = false) }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Timber.d("Service disconnected")
    }
}
