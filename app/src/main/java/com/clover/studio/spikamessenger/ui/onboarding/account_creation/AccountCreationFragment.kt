package com.clover.studio.spikamessenger.ui.onboarding.account_creation

import android.app.Activity
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.BuildConfig
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.databinding.FragmentAccountCreationBinding
import com.clover.studio.spikamessenger.ui.main.startMainActivity
import com.clover.studio.spikamessenger.ui.onboarding.OnboardingViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.FileUploadListener
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.Tools.convertBitmapToUri
import com.clover.studio.spikamessenger.utils.UploadDownloadManager
import com.clover.studio.spikamessenger.utils.dialog.ChooserDialog
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.getChunkSize
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class AccountCreationFragment : BaseFragment() {
    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    private val viewModel: OnboardingViewModel by activityViewModels()
    private var currentPhotoLocation: Uri = Uri.EMPTY

    private var progressAnimation: AnimationDrawable? = null

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it, false)
                val bitmapUri = convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this)
                    .load(bitmap)
                    .placeholder(R.drawable.img_user_avatar)
                    .centerCrop()
                    .into(binding.profilePicture.ivPickAvatar)
                currentPhotoLocation = bitmapUri
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
                val bitmapUri = convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap)
                    .placeholder(R.drawable.img_user_avatar)
                    .centerCrop()
                    .into(binding.profilePicture.ivPickAvatar)
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

        initializeViews()
        addTextListeners()
        addClickListeners()
        initializeObservers()

        viewModel.sendContacts()
        return binding.root
    }

    private fun initializeViews() {
        binding.profilePicture.ivProgressBar.apply {
            setBackgroundResource(R.drawable.drawable_progress_animation)
            progressAnimation = background as AnimationDrawable
        }
    }

    private fun initializeObservers() {
        viewModel.accountCreationListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> Timber.d("Contacts sent successfully")
                Resource.Status.ERROR -> Timber.d("Failed to send contacts")
                else -> Timber.d("Other error")
            }
        })

        viewModel.userUpdateListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    startMainActivity(requireActivity())
                }

                Resource.Status.ERROR -> Timber.d("Error updating user")
                else -> Timber.d("Other error")
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

        binding.profilePicture.ivPickAvatar.setOnClickListener {
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
    }

    private fun addTextListeners() {
        binding.etEnterUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Ignore
            }

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!text.isNullOrEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.cvUsernameError.visibility = View.INVISIBLE
                }
            }

            override fun afterTextChanged(text: Editable?) {
                if (!text.isNullOrEmpty()) {
                    binding.btnNext.isEnabled = true
                    binding.cvUsernameError.visibility = View.INVISIBLE
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
        binding.profilePicture.flProgressScreen.visibility = View.GONE
        progressAnimation?.stop()

        currentPhotoLocation = Uri.EMPTY
        Glide.with(this).clear(binding.profilePicture.ivPickAvatar)
        binding.profilePicture.ivPickAvatar.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.img_camera
            )
        )
    }

    private fun checkUsername() {
        if (binding.etEnterUsername.text.isNullOrEmpty()) {
            binding.btnNext.isEnabled = false
            binding.cvUsernameError.visibility = View.VISIBLE
        } else {
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
                        object : FileUploadListener {
                            override fun filePieceUploaded() {}

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
                                requireActivity().runOnUiThread {
                                    binding.profilePicture.flProgressScreen.visibility = View.GONE
                                    progressAnimation?.stop()
                                }

                                val jsonObject = JsonObject()
                                jsonObject.addProperty(
                                    Const.UserData.DISPLAY_NAME,
                                    binding.etEnterUsername.text.toString()
                                )
                                jsonObject.addProperty(Const.UserData.AVATAR_FILE_ID, fileId)

                                viewModel.updateUserData(jsonObject)
                            }

                            override fun fileCanceledListener(messageId: String?) {
                                // Ignore
                            }
                        })
                }
                binding.profilePicture.flProgressScreen.visibility = View.VISIBLE
                progressAnimation?.start()
            } else {
                val jsonObject = JsonObject()
                jsonObject.addProperty(
                    Const.UserData.DISPLAY_NAME,
                    binding.etEnterUsername.text.toString()
                )
                viewModel.updateUserData(jsonObject)
            }
        }
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
}
