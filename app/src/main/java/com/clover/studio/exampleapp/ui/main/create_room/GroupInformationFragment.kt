package com.clover.studio.exampleapp.ui.main.create_room

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
import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.entity.UserAndPhoneUser
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.FragmentGroupInformationBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.ui.main.chat.startChatScreenActivity
import com.clover.studio.exampleapp.utils.*
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GroupInformationFragment : BaseFragment() {
    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: GroupInformationAdapter
    private var selectedUsers: MutableList<UserAndPhoneUser> = ArrayList()
    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var progress: Long = 1L
    private var uploadPieces: Int = 0
    private var avatarFileId: Long? = null

    private var bindingSetup: FragmentGroupInformationBinding? = null

    private val binding get() = bindingSetup!!

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it, false)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).centerCrop().into(binding.ivPickPhoto)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
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

                Glide.with(this).load(bitmap).centerCrop().into(binding.ivPickPhoto)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
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
                object : DialogInteraction {
                    override fun onFirstOptionClicked() {
                        // ignore
                    }

                    override fun onSecondOptionClicked() {
                        // ignore
                    }
                })
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

    private fun initializeViews() {
        binding.tvCreate.setOnClickListener {
            val jsonObject = JsonObject()

            val userIdsArray = JsonArray()
            for (user in selectedUsers) {
                userIdsArray.add(user.user.id)
            }
            val adminUserIds = JsonArray()
            adminUserIds.add(viewModel.getLocalUserId())

            jsonObject.addProperty(
                Const.JsonFields.NAME,
                binding.etEnterUsername.text.toString()
            )
            jsonObject.addProperty(Const.JsonFields.AVATAR_FILE_ID, avatarFileId)
            jsonObject.add(Const.JsonFields.USER_IDS, userIdsArray)
            jsonObject.add(Const.JsonFields.ADMIN_USER_IDS, adminUserIds)
            jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.GROUP)

            showProgress(false)
            viewModel.createNewRoom(jsonObject)
        }

        binding.tvPeopleSelected.text = getString(R.string.s_people_selected, selectedUsers.size)
        adapter.submitList(selectedUsers)

        binding.etEnterUsername.addTextChangedListener {
            if (binding.etEnterUsername.text.isNotEmpty()) {
                binding.tvCreate.isClickable = true
                binding.tvCreate.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary_color
                    )
                )
            } else {
                binding.tvCreate.isClickable = false
                binding.tvCreate.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_tertiary
                    )
                )
            }
        }

        binding.etEnterUsername.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
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

        binding.ivCancel.setOnClickListener {
            requireActivity().onBackPressed()
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
                    Timber.d("Room with users: $roomWithUsers")
                    activity?.let { parent -> startChatScreenActivity(parent, roomWithUsers) }
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

        viewModel.mediaUploadListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.LOADING -> {
                    if (progress <= uploadPieces) {
                        binding.progressBar.secondaryProgress = progress.toInt()
                        progress++
                    } else progress = 0
                }
                Resource.Status.SUCCESS -> {
                    Timber.d("Upload verified")
                    requireActivity().runOnUiThread {
                        binding.clProgressScreen.visibility = View.GONE
                    }
                    avatarFileId = it.responseData?.fileId
                }
                Resource.Status.ERROR -> {
                    Timber.d("Upload Error")
                    requireActivity().runOnUiThread {
                        showUploadError(it.message!!)
                    }
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
        if (currentPhotoLocation != Uri.EMPTY) {
            val inputStream =
                requireActivity().contentResolver.openInputStream(currentPhotoLocation)

            val fileStream = Tools.copyStreamToFile(
                requireActivity(),
                inputStream!!,
                activity?.contentResolver?.getType(currentPhotoLocation)!!
            )
            uploadPieces =
                if ((fileStream.length() % getChunkSize(fileStream.length())).toInt() != 0)
                    (fileStream.length() / getChunkSize(fileStream.length()) + 1).toInt()
                else (fileStream.length() / getChunkSize(fileStream.length())).toInt()

            binding.progressBar.max = uploadPieces
            Timber.d("File upload start")
            viewModel.uploadMedia(
                requireActivity(),
                currentPhotoLocation,
                Const.JsonFields.AVATAR_TYPE,
                uploadPieces,
                fileStream,
                null,
                false
            )
            binding.clProgressScreen.visibility = View.VISIBLE
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

    private fun showRoomCreationError(description: String) {
        DialogError.getInstance(
            requireActivity(),
            getString(R.string.error),
            description,
            null,
            getString(R.string.ok),
            object : DialogInteraction {
                // ignore
            })
    }
}