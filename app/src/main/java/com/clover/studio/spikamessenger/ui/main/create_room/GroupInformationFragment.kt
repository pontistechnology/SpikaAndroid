package com.clover.studio.spikamessenger.ui.main.create_room

import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.BuildConfig
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentGroupInformationBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.chat.startChatScreenActivity
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.UploadDownloadManager
import com.clover.studio.spikamessenger.utils.dialog.ChooserDialog
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.getChunkSize
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GroupInformationFragment : BaseFragment() {
    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    private var bindingSetup: FragmentGroupInformationBinding? = null
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: GroupInformationAdapter
    private var selectedUsers: MutableList<PrivateGroupChats> = ArrayList()
    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var uploadPieces: Int = 0
    private var avatarFileId: Long? = null

    private var progressAnimation: AnimationDrawable? = null

    private val binding get() = bindingSetup!!

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it, false)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).centerCrop().into(binding.profilePicture.ivPickAvatar)
                currentPhotoLocation = bitmapUri
                updateGroupImage()
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

                Glide.with(this).load(bitmap).centerCrop().into(binding.profilePicture.ivPickAvatar)
                currentPhotoLocation = bitmapUri
                updateGroupImage()
            } else {
                Timber.d("Photo error")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireArguments().getParcelableArrayList<UserAndPhoneUser>(Const.Navigation.SELECTED_USERS) == null) {
            DialogError.getInstance(requireActivity(),
                getString(R.string.error),
                getString(R.string.failed_user_data),
                null,
                getString(R.string.ok),
                object : DialogInteraction {})
            Timber.d("Failed to fetch user data")
        } else {
            selectedUsers =
                requireArguments().getParcelableArrayList(Const.Navigation.SELECTED_USERS)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentGroupInformationBinding.inflate(inflater, container, false)

        setupAdapter()
        initializeObservers()
        initializeViews()

        return binding.root
    }

    private fun initializeViews() = with(binding) {
        profilePicture.ivProgressBar.apply {
            setBackgroundResource(R.drawable.drawable_progress_animation)
            progressAnimation = background as AnimationDrawable
        }

        profilePicture.ivPickAvatar.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.img_group_avatar
            )
        )
        setUpDoneButton(uploading = false)

        tvPeopleSelected.text = getString(R.string.s_people_selected, selectedUsers.size)
        adapter.submitList(selectedUsers)

        etEnterUsername.addTextChangedListener {
            fabDone.visibility = if (etEnterUsername.text.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Set title of the screen to room name if not empty.
            tvTitle.text = if (etEnterUsername.text.isNotEmpty()) {
                etEnterUsername.text.toString()
            } else {
                getString(R.string.name_the_group)
            }
        }

        etEnterUsername.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
        }

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

        ivCancel.setOnClickListener {
            goBack()
        }

        ivAddMoreUsers.setOnClickListener {
            goBack()
        }
    }

    private fun setUpDoneButton(uploading: Boolean) = with(binding) {
        fabDone.isEnabled = !uploading
        if (uploading) fabDone.alpha = 0.5f else fabDone.alpha = 1f

        Timber.d("Is enabled: ${fabDone.isEnabled}")

        if (fabDone.isEnabled) {
            fabDone.setOnClickListener {
                val jsonObject = JsonObject()

                val userIdsArray = JsonArray()
                for (user in selectedUsers) {
                    userIdsArray.add(user.userId)
                }
                val adminUserIds = JsonArray()
                adminUserIds.add(viewModel.getLocalUserId())

                jsonObject.addProperty(
                    Const.JsonFields.NAME,
                    etEnterUsername.text.toString().trim()
                )
                jsonObject.addProperty(Const.JsonFields.AVATAR_FILE_ID, avatarFileId)
                jsonObject.add(Const.JsonFields.USER_IDS, userIdsArray)
                jsonObject.add(Const.JsonFields.ADMIN_USER_IDS, adminUserIds)
                jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.GROUP)

                showProgress(false)
                viewModel.createNewRoom(jsonObject)
                viewModel.roomUsers.clear()
            }
        }
    }

    private fun initializeObservers() {
        viewModel.createRoomListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    hideProgress()
                    val users = mutableListOf<User>()
                    for (roomUser in it.responseData?.data?.room!!.users) {
                        roomUser.user?.let { user -> users.add(user) }
                    }

                    val roomWithUsers = RoomWithUsers(it.responseData.data.room, users)
                    activity?.let { parent ->
                        startChatScreenActivity(
                            parent,
                            roomWithUsers
                        )
                    }
                    findNavController().popBackStack(R.id.mainFragment, false)
                }

                Resource.Status.ERROR -> {
                    hideProgress()
                    showRoomCreationError(getString(R.string.failed_room_creation))
                    Timber.d("Failed to create room")
                }

                else -> {
                    hideProgress()
                    showRoomCreationError(getString(R.string.something_went_wrong))
                    Timber.d("Other error")
                }
            }
        })

        viewModel.fileUploadListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.LOADING -> {
                    setUpDoneButton(uploading = true)
                }

                Resource.Status.SUCCESS -> {
                    Timber.d("Upload verified")
                    requireActivity().runOnUiThread {
                        binding.profilePicture.flProgressScreen.visibility = View.GONE
                        progressAnimation?.stop()
                    }
                    avatarFileId = it.responseData?.fileId
                    setUpDoneButton(uploading = false)
                }

                Resource.Status.ERROR -> {
                    Timber.d("Upload Error")
                    requireActivity().runOnUiThread {
                        showUploadError(it.message!!)
                    }
                    setUpDoneButton(uploading = false)
                }

                else -> Toast.makeText(
                    requireContext(),
                    getString(R.string.something_went_wrong),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupAdapter() {
        adapter = GroupInformationAdapter(requireContext()) {
            selectedUsers.remove(it)
            adapter.submitList(selectedUsers)
            binding.tvPeopleSelected.text =
                getString(R.string.s_people_selected, selectedUsers.size)
            adapter.notifyDataSetChanged()
            viewModel.roomUsers.remove(it)
        }

        binding.rvContacts.adapter = adapter
        binding.rvContacts.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
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

    private fun updateGroupImage() {
        setUpDoneButton(uploading = true)
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

            Timber.d("File upload start")
            viewModel.uploadMedia(
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
                )
            )
            binding.profilePicture.flProgressScreen.visibility = View.VISIBLE
            progressAnimation?.start()
            inputStream.close()
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
                    // Ignore
                }

                override fun onSecondOptionClicked() {
                    // Ignore
                }
            })
        binding.profilePicture.flProgressScreen.visibility = View.GONE
        progressAnimation?.stop()
        currentPhotoLocation = Uri.EMPTY
        Glide.with(this).clear(binding.profilePicture.ivPickAvatar)
    }

    private fun showRoomCreationError(description: String) {
        DialogError.getInstance(
            requireActivity(),
            getString(R.string.error),
            description,
            null,
            getString(R.string.ok),
            object : DialogInteraction {
                // Ignore
            })
    }

    private fun goBack() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }
}
