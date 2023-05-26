package com.clover.studio.exampleapp.ui.main.chat_details

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clover.studio.exampleapp.BuildConfig
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.databinding.FragmentChatDetailsBinding
import com.clover.studio.exampleapp.ui.main.chat.ChatViewModel
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.UploadDownloadManager
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.clover.studio.exampleapp.utils.getChunkSize
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ChatDetailsFragment : BaseFragment() {

    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    @Inject
    lateinit var sharedPrefs: SharedPreferencesRepository

    private val viewModel: ChatViewModel by activityViewModels()
    private val args: ChatDetailsFragmentArgs by navArgs()
    private lateinit var adapter: ChatDetailsAdapter
    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var roomUsers: MutableList<User> = ArrayList()
    private lateinit var roomWithUsers: RoomWithUsers
    private var progress: Long = 1L
    private var uploadPieces: Int = 0
    private var roomId: Int? = null
    private var isAdmin = false

    private var userName = ""
    private var avatarFileId = 0L
    private var newAvatarFileId = 0L
    private var isUploading = false

    private var bindingSetup: FragmentChatDetailsBinding? = null
    private val binding get() = bindingSetup!!

    private var allUsers = false
    private var modifiedList: List<User> = mutableListOf()

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it, false)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).centerCrop().into(binding.ivPickAvatar)
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

                Glide.with(this).load(bitmap).centerCrop().into(binding.ivPickAvatar)
                binding.clSmallCameraPicker.visibility = View.VISIBLE
                currentPhotoLocation = bitmapUri
                updateGroupImage()
            } else {
                Timber.d("Photo error")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fetch room data sent from previous fragment
        roomWithUsers = args.roomWithUsers
        isAdmin = args.isAdmin
        roomId = roomWithUsers.room.roomId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentChatDetailsBinding.inflate(inflater, container, false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        initializeViews(roomWithUsers)
        initializeObservers()
        handleUserStatusViews(isAdmin)

        return binding.root
    }

    private fun handleUserStatusViews(isAdmin: Boolean) {
        if (!isAdmin) {
            binding.tvGroupName.isClickable = false
            binding.tvDone.isFocusable = false
            binding.ivPickAvatar.isClickable = false
            binding.ivPickAvatar.isFocusable = false
            binding.ivAddMember.visibility = View.GONE
        }
    }

    private fun initializeViews(roomWithUsers: RoomWithUsers) {
        setupAdapter(isAdmin, roomWithUsers.room.type.toString())
        binding.clMemberList.visibility = View.VISIBLE
        userName = roomWithUsers.room.name.toString()
        avatarFileId = roomWithUsers.room.avatarFileId!!

        binding.tvMembersNumber.text =
            getString(R.string.number_of_members, roomWithUsers.users.size)

        if (isAdmin) {
            binding.tvDelete.visibility = View.VISIBLE
            binding.ivAddMember.visibility = View.VISIBLE
        }

        if (!roomWithUsers.room.roomExit) {
            binding.tvExitGroup.visibility = View.VISIBLE
        } else {
            binding.tvExitGroup.visibility = View.GONE
        }

        binding.chatHeader.tvTitle.text = roomWithUsers.room.type

        // Set room muted or not muted on switch
        binding.swMute.isChecked = roomWithUsers.room.muted

        // Set room pinned or not pinned on switch
        binding.swPinChat.isChecked = roomWithUsers.room.pinned

        // This will stop image file changes while file is uploading via LiveData
        if (!isUploading && binding.tvDone.visibility == View.GONE) {
            setAvatarAndUsername(avatarFileId, userName)
        }
        initializeListeners(roomWithUsers)
    }

    private fun initializeListeners(roomWithUsers: RoomWithUsers) {
        binding.ivAddMember.setOnClickListener {
            val userIds = ArrayList<Int>()
            for (user in roomWithUsers.users) {
                userIds.add(user.id)
            }

            findNavController().navigate(
                ChatDetailsFragmentDirections.actionChatDetailsFragmentToNewRoomFragment2(
                    roomWithUsers.room.roomId,
                    userIds.stream().mapToInt { i -> i }.toArray()
                )
            )
        }

        binding.tvGroupName.setOnClickListener {
            if (roomWithUsers.room.type.toString() == Const.JsonFields.GROUP && isAdmin) {
                binding.etEnterGroupName.visibility = View.VISIBLE
                binding.tvDone.visibility = View.VISIBLE
                binding.tvGroupName.visibility = View.INVISIBLE
                binding.chatHeader.ivCallUser.visibility = View.INVISIBLE
                binding.chatHeader.ivVideoCall.visibility = View.INVISIBLE
            }
        }

        binding.tvDone.setOnClickListener {
            val roomName = binding.etEnterGroupName.text.toString()
//          val adminIds: MutableList<Int> = ArrayList()
            val jsonObject = JsonObject()
            if (roomName.isNotEmpty()) {
                jsonObject.addProperty(Const.JsonFields.NAME, roomName)
            }

            if (newAvatarFileId != 0L) {
                jsonObject.addProperty(Const.JsonFields.AVATAR_FILE_ID, newAvatarFileId)
            }

            viewModel.updateRoom(jsonObject, roomWithUsers.room.roomId, 0)

            binding.tvDone.visibility = View.GONE
            binding.chatHeader.ivCallUser.visibility = View.VISIBLE
            binding.chatHeader.ivVideoCall.visibility = View.VISIBLE
            binding.etEnterGroupName.visibility = View.INVISIBLE
            binding.tvGroupName.visibility = View.VISIBLE
        }

        binding.clNotes.setOnClickListener {
            val action = roomId?.let { id ->
                ChatDetailsFragmentDirections.actionChatDetailsFragmentToNotesFragment(
                    id
                )
            }
            if (action != null) {
                findNavController().navigate(action)
            }
        }

        binding.ivPickAvatar.setOnClickListener {
            if ((Const.JsonFields.GROUP == roomWithUsers.room.type) && isAdmin) {
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

        binding.chatHeader.ivArrowBack.setOnClickListener {
            val action =
                ChatDetailsFragmentDirections.actionChatDetailsFragmentToChatMessagesFragment2()
            findNavController().navigate(action)

        }

        binding.swMute.setOnCheckedChangeListener(multiListener)

        binding.swPinChat.setOnCheckedChangeListener(multiListener)

        // Rooms can only be deleted by room admins.
        binding.tvDelete.setOnClickListener {
            if (isAdmin) {
                DialogError.getInstance(requireActivity(),
                    getString(R.string.delete_chat),
                    getString(R.string.delete_chat_description),
                    getString(
                        R.string.yes
                    ),
                    getString(R.string.no),
                    object : DialogInteraction {
                        override fun onFirstOptionClicked() {
                            roomId?.let { id -> viewModel.deleteRoom(id) }
                            activity?.finish()
                        }
                    })
            }
        }

        binding.tvExitGroup.setOnClickListener {
            val adminIds = ArrayList<Int>()
            for (user in roomUsers) {
                if (user.isAdmin)
                    adminIds.add(user.id)
            }

            // Exit condition:
            // If current user is not admin
            // Or current user is admin and and there are other admins
            if (!isAdmin || (adminIds.size > 1) && isAdmin) {
                DialogError.getInstance(requireActivity(),
                    getString(R.string.exit_group),
                    getString(R.string.exit_group_description),
                    getString(R.string.cancel),
                    getString(
                        R.string.exit
                    ),
                    object : DialogInteraction {
                        override fun onSecondOptionClicked() {
                            val myId = viewModel.getLocalUserId()
                            roomId?.let { id -> viewModel.leaveRoom(id) }
                            // Remove if admin
                            if (isAdmin) {
                                roomId?.let { id -> viewModel.removeAdmin(id, myId!!) }
                            }
                            activity?.finish()
                        }
                    })
            } else {
                DialogError.getInstance(requireActivity(),
                    getString(R.string.exit_group),
                    getString(R.string.exit_group_error),
                    null,
                    getString(R.string.ok),
                    object : DialogInteraction {
                        override fun onSecondOptionClicked() {
                            // Ignore
                        }
                    })
            }
        }

        binding.tvSeeMoreLess.setOnClickListener {
            if (allUsers) {
                adapter.submitList(modifiedList.toList())
                binding.tvSeeMoreLess.text = context!!.getString(R.string.see_less)
                allUsers = false
            } else {
                adapter.submitList(modifiedList.subList(0, 3).toList())
                binding.tvSeeMoreLess.text = context!!.getString(R.string.see_more)
                allUsers = true
            }
        }
    }

    // Listener which handles switch events and sends event to specific switch
    private val multiListener: OnCheckedChangeListener =
        OnCheckedChangeListener { buttonView, isChecked ->
            when (buttonView.id) {
                binding.swPinChat.id -> {
                    if (buttonView.isPressed) {
                        if (isChecked) {
                            roomId?.let { viewModel.handleRoomPin(it, true) }
                        } else {
                            roomId?.let { viewModel.handleRoomPin(it, false) }
                        }
                    }
                }

                binding.swMute.id -> {
                    if (buttonView.isPressed) {
                        if (isChecked) {
                            roomId?.let { viewModel.handleRoomMute(it, true) }
                        } else {
                            roomId?.let { viewModel.handleRoomMute(it, false) }
                        }
                    }
                }
            }
        }

    private fun setAvatarAndUsername(avatarFileId: Long, username: String) {
        if (avatarFileId != 0L) {
            Glide.with(this)
                .load(avatarFileId.let { Tools.getFilePathUrl(it) })
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.ivPickAvatar)
            Glide.with(this)
                .load(avatarFileId.let { Tools.getFilePathUrl(it) })
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.chatHeader.ivUserImage)
        }
        binding.tvGroupName.text = username
        binding.chatHeader.tvChatName.text = username
    }

    private fun initializeObservers() {
        roomId?.let {
            viewModel.getRoomAndUsers(it).observe(viewLifecycleOwner) { data ->
                when (data.status) {
                    Resource.Status.SUCCESS -> {
                        val roomWithUsers = data.responseData
                        if (roomWithUsers != null) {
                            initializeViews(roomWithUsers)
                            if (Const.JsonFields.GROUP == roomWithUsers.room.type) {
                                updateRoomUserList(roomWithUsers)
                            }
                        }
                    }

                    Resource.Status.LOADING -> {
                        // Add loading bar
                    }

                    else -> {
                        Timber.d("Error: $data")
                    }
                }
            }
        }

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
                        binding.chatHeader.ivVideoCall.visibility = View.INVISIBLE
                        binding.chatHeader.ivCallUser.visibility = View.INVISIBLE
                        binding.tvDone.visibility = View.VISIBLE
                    }
                    newAvatarFileId = it.responseData!!.fileId
                    isUploading = false
                }

                Resource.Status.ERROR -> {
                    Timber.d("Upload Error")
                    requireActivity().runOnUiThread {
                        showUploadError(it.message!!)
                    }
                }

                else -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.something_went_wrong),
                        Toast.LENGTH_SHORT
                    ).show()
                    isUploading = false
                }
            }
        })
    }

    private fun setupAdapter(isAdmin: Boolean, roomType: String) {
        adapter = ChatDetailsAdapter(
            requireContext(),
            isAdmin,
            roomType,
            onUserInteraction = { event, user ->
                when (event) {
                    Const.UserActions.USER_OPTIONS -> userActions(user, roomType)
                    Const.UserActions.USER_REMOVE -> removeUser(user)
                    else -> Timber.d("No other action currently")
                }
            }
        )

        // binding.rvGroupMembers.itemAnimator = null
        binding.rvGroupMembers.adapter = adapter
        binding.rvGroupMembers.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
    }

    private fun userActions(user: User, roomType: String) {
        val adminText = if (Const.JsonFields.GROUP == roomType) {
            if (isAdmin && !user.isAdmin) {
                getString(R.string.make_group_admin)
            } else {
                null
            }
        } else {
            null
        }

        user.displayName?.let {
            ChooserDialog.getInstance(requireContext(),
                it,
                null,
                getString(R.string.info),
                adminText,
                object : DialogInteraction {
                    override fun onFirstOptionClicked() {
                        // TODO("Not yet implemented")
                    }

                    override fun onSecondOptionClicked() {
                        user.isAdmin = true
                        makeAdmin()
                        modifiedList =
                            roomUsers.sortedBy { roomUser -> roomUser.isAdmin }.reversed()
                        adapter.submitList(modifiedList.toList())
                        adapter.notifyDataSetChanged()
                    }

                })
        }
    }

    private fun removeUser(user: User) {
        roomUsers.remove(user)
        updateRoomUsers(user.id)
        modifiedList =
            roomUsers.sortedBy { roomUser -> roomUser.isAdmin }.reversed()
        adapter.submitList(modifiedList.toList())
        adapter.notifyDataSetChanged()
    }

    private fun makeAdmin() {
        val jsonObject = JsonObject()
        val adminIds = JsonArray()

        for (user in roomUsers) {
            if (user.isAdmin)
                adminIds.add(user.id)
        }

        if (adminIds.size() > 0)
            jsonObject.add(Const.JsonFields.ADMIN_USER_IDS, adminIds)

        roomId?.let { viewModel.updateRoom(jsonObject, it, 0) }

    }

    private fun updateRoomUserList(roomWithUsers: RoomWithUsers) {
        roomUsers.clear()
        roomUsers.addAll(roomWithUsers.users)
        runBlocking {
            for (user in roomUsers) {
                if (roomId?.let { viewModel.isUserAdmin(it, user.id) } == true) {
                    user.isAdmin = true
                }
            }
            modifiedList = roomUsers.sortedBy { user -> user.isAdmin }.reversed()
            if (modifiedList.size > 3) {
                adapter.submitList(modifiedList.subList(0, 3).toList())
                binding.tvSeeMoreLess.visibility = View.VISIBLE
                binding.tvSeeMoreLess.text = context!!.getString(R.string.see_more)
                allUsers = true
            } else {
                adapter.submitList(modifiedList.toList())
                binding.tvSeeMoreLess.visibility = View.GONE
            }
        }
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
            isUploading = true
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
        Glide.with(this).clear(binding.ivPickAvatar)
        binding.ivPickAvatar.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.img_camera
            )
        )
        binding.clSmallCameraPicker.visibility = View.GONE
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

    private fun updateRoomUsers(idToRemove: Int) {
        val jsonObject = JsonObject()
        val userIds = JsonArray()

        for (user in roomUsers) {
            if (!user.isAdmin)
                userIds.add(user.id)
        }

        jsonObject.add(Const.JsonFields.USER_IDS, userIds)

        roomId?.let { viewModel.updateRoom(jsonObject, it, idToRemove) }
    }
}