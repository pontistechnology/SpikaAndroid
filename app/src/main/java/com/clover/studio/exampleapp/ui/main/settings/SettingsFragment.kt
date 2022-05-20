package com.clover.studio.exampleapp.ui.main.settings

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
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
import com.clover.studio.exampleapp.data.models.UploadFile
import com.clover.studio.exampleapp.databinding.FragmentSettingsBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.ui.main.UserUpdateFailed
import com.clover.studio.exampleapp.ui.main.UserUpdated
import com.clover.studio.exampleapp.ui.onboarding.*
import com.clover.studio.exampleapp.ui.onboarding.account_creation.CHUNK_SIZE
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.Tools.getAvatarUrl
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.dialog.DialogInteraction
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*

class SettingsFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var bindingSetup: FragmentSettingsBinding? = null
    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var uploadPieces: Long = 0L
    private var progress: Long = 1L
    private var uploadFile: UploadFile? = null

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

            Glide.with(requireActivity()).load(it.avatarUrl?.let { url -> getAvatarUrl(url) })
                .into(binding.ivPickPhoto)
        }

        viewModel.userUpdateListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                UserUpdated -> {
                    binding.tvUsername.visibility = View.VISIBLE
                    binding.tvPhoneNumber.visibility = View.VISIBLE
                    binding.etEnterUsername.visibility = View.INVISIBLE
                    binding.tvDone.visibility = View.GONE
                }
                UserUpdateFailed -> Timber.d("User update failed")
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
                        Const.UserData.AVATAR_URL to it.path
                    )
                    binding.clProgressScreen.visibility = View.GONE
                    viewModel.updateUserData(userData)
                }
                UploadVerificationFailed -> showUploadError()
                UploadError -> showUploadError()
            }
        })
    }

    private fun setupClickListeners() {
        binding.clPrivacy.setOnClickListener {
            goToPrivacySettings()
        }

        binding.clChat.setOnClickListener {
            goToChatSettings()
        }

        binding.clNotifications.setOnClickListener {
            goToNotificationSettings()
        }

        binding.clMediaDownload.setOnClickListener {
            goToDownloadSettings()
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
            Timber.d("File upload start")
            uploadFile(Tools.copyStreamToFile(requireActivity(), inputStream!!))
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
                    Tools.sha256HashFromUri(requireActivity(), currentPhotoLocation),
                    Const.JsonFields.AVATAR,
                    1
                )

                viewModel.uploadFile(uploadFile!!.chunkToJson(), uploadPieces)

                Timber.d("$uploadFile")
                piece++
            }
        }
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
}