package com.clover.studio.spikamessenger.ui.main.chat_details

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clover.studio.spikamessenger.BuildConfig
import com.clover.studio.spikamessenger.MainApplication
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.FileData
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.data.repositories.SharedPreferencesRepository
import com.clover.studio.spikamessenger.databinding.FragmentChatDetailsBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.UserOptions
import com.clover.studio.spikamessenger.utils.dialog.ChooserDialog
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.getChunkSize
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.clover.studio.spikamessenger.utils.helpers.UploadService
import com.clover.studio.spikamessenger.utils.helpers.UserOptionsData
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

const val MIN_ADMIN_NUMBER = 1

@AndroidEntryPoint
class ChatDetailsFragment : BaseFragment(), ServiceConnection {

    @Inject
    lateinit var sharedPrefs: SharedPreferencesRepository

    private val viewModel: ChatViewModel by activityViewModels()
    private val args: ChatDetailsFragmentArgs by navArgs()
    private var adapter: ChatDetailsAdapter? = null

    private var roomUsers: MutableList<User> = mutableListOf()
    private var modifiedList: List<User> = mutableListOf()
    private var roomWithUsers: RoomWithUsers? = null

    private var currentPhotoLocation: Uri = Uri.EMPTY
    private var uploadPieces: Int = 0
    private lateinit var fileUploadService: UploadService
    private var bound = false
    private var isUploading = false

    private var roomId: Int? = null
    private var isAdmin = false
    private var localUserId: Int? = 0
    private var userName = ""
    private var avatarFileId = 0L

    private var bindingSetup: FragmentChatDetailsBinding? = null
    private val binding get() = bindingSetup!!

    private var optionList: MutableList<UserOptionsData> = mutableListOf()
    private var pinSwitch: Drawable? = null
    private var muteSwitch: Drawable? = null
    private var avatarData: FileData? = null

    private var navOptionsBuilder: NavOptions? = null

    private var progressAnimation: AnimationDrawable? = null

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it, false)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this)
                    .load(bitmap)
                    .centerCrop()
                    .into(binding.profilePicture.ivPickAvatar)
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
        // Fetch room data sent from previous fragment
        roomWithUsers = args.roomWithUsers
        localUserId = viewModel.getLocalUserId()
        isAdmin = roomWithUsers?.users?.any { user ->
            user.id == localUserId && roomWithUsers?.room?.roomId?.let {
                viewModel.isUserAdmin(
                    it,
                    user.id
                )
            } == true
        } == true
        roomId = roomWithUsers?.room?.roomId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentChatDetailsBinding.inflate(inflater, container, false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        navOptionsBuilder = Tools.createCustomNavOptions()

        handleUserStatusViews(isAdmin)
        initializeViews(roomWithUsers)

        setOptionList()

        initializeObservers()

        return binding.root
    }

    private fun goToExitGroup() {
        val adminIds = ArrayList<Int>()
        for (user in roomUsers) {
            if (user.isAdmin)
                adminIds.add(user.id)
        }

        // Exit condition:
        // If current user is not admin
        // Or current user is admin and and there are other admins
        if (!isAdmin || (adminIds.size > MIN_ADMIN_NUMBER) && isAdmin) {
            DialogError.getInstance(requireActivity(),
                getString(R.string.exit_group),
                getString(R.string.exit_group_description),
                getString(R.string.cancel),
                getString(
                    R.string.exit
                ),
                object : DialogInteraction {
                    override fun onSecondOptionClicked() {
                        roomId?.let { id -> viewModel.leaveRoom(id) }
                        // Remove if admin
                        if (isAdmin) {
                            roomId?.let { id -> viewModel.removeAdmin(id, localUserId!!) }
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

    private fun goToDeleteChat() {
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

    private fun handleUserStatusViews(isAdmin: Boolean) = with(binding) {
        if (!isAdmin) {
            tvGroupName.isClickable = false
            ivDone.isFocusable = false
            profilePicture.ivPickAvatar.isClickable = false
            profilePicture.ivPickAvatar.isFocusable = false
            ivAddMember.visibility = View.GONE
        }
    }

    private fun initializeViews(roomWithUsers: RoomWithUsers?) = with(binding) {
        setupAdapter(isAdmin = isAdmin, roomType = roomWithUsers?.room?.type.toString())

        profilePicture.ivProgressBar.apply {
            setBackgroundResource(R.drawable.drawable_progress_animation)
            progressAnimation = background as AnimationDrawable
        }

        clMemberList.visibility = View.VISIBLE
        userName = roomWithUsers?.room?.name.toString()
        avatarFileId = roomWithUsers?.room?.avatarFileId!!

        tvMembersNumber.text =
            getString(R.string.number_of_members, roomWithUsers.users.size)

        // This will stop image file changes while file is uploading via LiveData
        if (!isUploading && ivDone.visibility == View.GONE) {
            setAvatarAndUsername(avatarFileId, userName)
        }

        initializeListeners(roomWithUsers)
    }

    private fun switchPinMuteOptions(optionName: String, isSwitched: Boolean) {
        roomWithUsers?.room?.roomId?.let { roomId ->
            when (optionName) {
                getString(R.string.pin_chat) -> viewModel.handleRoomPin(roomId, isSwitched)
                else -> viewModel.handleRoomMute(roomId, isSwitched)
            }
        }
    }

    private fun setOptionList() = with(binding) {
        val pinId =
            if (roomWithUsers?.room?.pinned == true) R.drawable.img_switch else R.drawable.img_switch_left
        val muteId =
            if (roomWithUsers?.room?.muted == true) R.drawable.img_switch else R.drawable.img_switch_left

        pinSwitch = AppCompatResources.getDrawable(requireContext(), pinId)
        muteSwitch = AppCompatResources.getDrawable(requireContext(), muteId)

        optionList = mutableListOf(
            UserOptionsData(
                option = getString(R.string.notes),
                firstDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.img_notes
                ),
                secondDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.img_arrow_forward
                ),
                switchOption = false,
                isSwitched = false,
            ),
            UserOptionsData(
                option = getString(R.string.pin_chat),
                firstDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.img_pin
                ),
                secondDrawable = pinSwitch,
                switchOption = true,
                isSwitched = roomWithUsers?.room?.pinned ?: false
            ),
            UserOptionsData(
                option = getString(R.string.mute),
                firstDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.img_mute
                ),
                secondDrawable = muteSwitch,
                switchOption = true,
                isSwitched = roomWithUsers?.room?.pinned ?: false
            ),
        )

        if (isAdmin) {
            optionList.add(
                UserOptionsData(
                    option = getString(R.string.delete_chat),
                    firstDrawable = AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.img_delete_note
                    ),
                    secondDrawable = null,
                    switchOption = false,
                    isSwitched = false
                )
            )
            ivAddMember.visibility = View.VISIBLE

        }

        if (roomWithUsers?.room?.roomExit == false) {
            optionList.add(
                UserOptionsData(
                    option = getString(R.string.exit_group),
                    firstDrawable = AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.img_chat_exit
                    ),
                    secondDrawable = null,
                    switchOption = false,
                    isSwitched = false
                )
            )
        }
        setOptionContainer()
    }

    private fun setOptionContainer() {
        val userOptions = UserOptions(requireContext())
        userOptions.setOptions(optionList)
        userOptions.setOptionsListener(object : UserOptions.OptionsListener {
            override fun clickedOption(option: Int, optionName: String) {
                when (optionName) {
                    getString(R.string.notes) -> goToNotes()
                    getString(R.string.delete_chat) -> goToDeleteChat()
                    getString(R.string.exit_group) -> goToExitGroup()
                }
            }

            override fun switchOption(optionName: String, isSwitched: Boolean) {
                switchPinMuteOptions(optionName, isSwitched)
            }
        })
        binding.flOptionsContainer.addView(userOptions)
    }

    private fun initializeListeners(roomWithUsers: RoomWithUsers) = with(binding) {
        ivAddMember.setOnClickListener {
            val userIds = ArrayList<Int>()
            for (user in roomWithUsers.users) {
                userIds.add(user.id)
            }

            findNavController().navigate(
                ChatDetailsFragmentDirections.actionChatDetailsFragmentToNewRoomFragment(
                    userIds.stream().mapToInt { i -> i }.toArray(),
                    roomWithUsers.room.roomId
                ),
                navOptionsBuilder
            )
        }

        tvGroupName.setOnClickListener {
            if (roomWithUsers.room.type.toString() == Const.JsonFields.GROUP && isAdmin) {
                tilEnterGroupName.visibility = View.VISIBLE
                etEnterGroupName.setText(tvGroupName.text)
                showKeyboard(etEnterGroupName)
                tvGroupPlaceholder.visibility = View.GONE
                ivDone.visibility = View.VISIBLE
                tvGroupName.visibility = View.GONE
            }
        }

        ivDone.setOnClickListener {
            val roomName = etEnterGroupName.text.toString()
            val jsonObject = JsonObject()
            if (roomName.isNotEmpty()) {
                jsonObject.addProperty(Const.JsonFields.NAME, roomName)
            }

            jsonObject.addProperty(Const.JsonFields.ACTION, Const.JsonFields.CHANGE_GROUP_NAME)

            viewModel.updateRoom(jsonObject = jsonObject, roomId = roomWithUsers.room.roomId)

            ivDone.visibility = View.GONE
            tilEnterGroupName.visibility = View.GONE
            tvGroupPlaceholder.visibility = View.VISIBLE
            tvGroupName.visibility = View.VISIBLE
        }

        profilePicture.ivPickAvatar.setOnClickListener {
            if ((Const.JsonFields.GROUP == roomWithUsers.room.type) && isAdmin) {
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

        binding.ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun goToNotes() {
        val action = roomId?.let { id ->
            ChatDetailsFragmentDirections.actionChatDetailsFragmentToNotesFragment(
                id
            )
        }
        if (action != null) {
            findNavController().navigate(action)
        }
    }

    private fun setAvatarAndUsername(avatarFileId: Long, username: String) {
        if (avatarFileId != 0L) {
            Glide.with(this)
                .load(Tools.getFilePathUrl(avatarFileId))
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.profilePicture.ivPickAvatar)
        } else {
            binding.profilePicture.ivPickAvatar.setImageResource(R.drawable.img_group_avatar)
        }

        binding.tvGroupName.text = username
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

        binding.rvGroupMembers.adapter = adapter
        binding.rvGroupMembers.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
    }

    private fun userActions(user: User, roomType: String) {
        val adminText = when {
            Const.JsonFields.GROUP == roomType && isAdmin && !user.isAdmin -> getString(R.string.make_group_admin)
            Const.JsonFields.GROUP == roomType && isAdmin && roomUsers.count { it.isAdmin } > MIN_ADMIN_NUMBER -> getString(
                R.string.dismiss_as_admin
            )

            else -> null
        }

        if (adminText == null && user.id == localUserId) return

        user.formattedDisplayName.let {
            val listOptions = mutableListOf<Pair<String, () -> Unit>>()

            if (adminText != null)
                listOptions.add(adminText to { checkAdmin(user = user, adminText = adminText) })

            if (user.id != localUserId)
                listOptions.add(getString(R.string.info) to { userProfileNavigation(user = user) })

            listOptions.add(getString(R.string.cancel) to {})

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

    private fun userProfileNavigation(user: User) {
        val privateGroupUser = Tools.transformUserToPrivateGroupChat(user)
        val bundle =
            bundleOf(
                Const.Navigation.USER_PROFILE to privateGroupUser,
                Const.Navigation.ROOM_ID to roomWithUsers?.room?.roomId,
                Const.Navigation.ROOM_DATA to roomWithUsers?.room
            )
        findNavController().navigate(
            R.id.action_chatDetailsFragment_to_contactDetailsFragment,
            bundle,
            navOptionsBuilder
        )
    }

    private fun checkAdmin(user: User, adminText: String) {
        if (getString(R.string.dismiss_as_admin) == adminText) {
            user.isAdmin = false
            removeAdmin(user.id)
        } else {
            user.isAdmin = true
            makeAdmin(user.id)
        }

        modifiedList =
            roomUsers.sortedBy { roomUser -> roomUser.isAdmin }.reversed()
        adapter?.submitList(modifiedList.toList())

        adapter?.notifyItemChanged(modifiedList.indexOf(user))
    }

    private fun removeUser(user: User) {
        DialogError.getInstance(requireActivity(),
            getString(R.string.remove_from_group),
            getString(R.string.remove_person, user.formattedDisplayName),
            getString(R.string.cancel),
            getString(R.string.ok),
            object : DialogInteraction {
                override fun onSecondOptionClicked() {
                    roomUsers.remove(user)
                    updateRoomUsers(user.id)
                }
            })
    }

    private fun makeAdmin(userId: Int) {
        val jsonObject = JsonObject()
        val adminIds = JsonArray()

        adminIds.add(userId)

        if (adminIds.size() > 0) {
            jsonObject.addProperty(Const.JsonFields.ACTION, Const.JsonFields.ADD_GROUP_ADMINS)
            jsonObject.add(Const.JsonFields.USER_IDS, adminIds)
        }

        roomId?.let { viewModel.updateRoom(jsonObject = jsonObject, roomId = it) }
    }

    private fun removeAdmin(userId: Int) {
        val jsonObject = JsonObject()
        val adminIds = JsonArray()

        adminIds.add(userId)

        if (adminIds.size() > 0) {
            jsonObject.addProperty(Const.JsonFields.ACTION, Const.JsonFields.REMOVE_GROUP_ADMINS)
            jsonObject.add(Const.JsonFields.USER_IDS, adminIds)
        }

        roomId?.let { viewModel.updateRoom(jsonObject = jsonObject, roomId = it) }
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
            adapter?.submitList(modifiedList.toList())

        }
    }

    private fun updateGroupImage() {
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

            progressAnimation?.start()
            isUploading = true

            avatarData = FileData(
                fileUri = currentPhotoLocation,
                fileType = Const.JsonFields.AVATAR_TYPE,
                filePieces = uploadPieces,
                file = fileStream,
                messageBody = null,
                isThumbnail = false,
                localId = null,
                roomId = roomWithUsers?.room?.roomId ?: 0,
                messageStatus = null,
                metadata = null
            )

            if (bound) {
                CoroutineScope(Dispatchers.Default).launch {
                    avatarData?.let {
                        fileUploadService.uploadAvatar(
                            fileData = it,
                            isGroup = true
                        )
                    }
                }
            } else {
                startUploadService()
            }
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
        binding.profilePicture.ivPickAvatar.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.img_group_avatar
            )
        )
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

        userIds.add(idToRemove)
        jsonObject.addProperty(Const.JsonFields.ACTION, Const.JsonFields.REMOVE_GROUP_USERS)
        jsonObject.add(Const.JsonFields.USER_IDS, userIds)

        roomId?.let { viewModel.updateRoom(jsonObject = jsonObject, roomId = it) }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            requireActivity().unbindService(serviceConnection)
        }
        bound = false
    }

    /** Upload service */
    private fun startUploadService() {
        val intent = Intent(MainApplication.appContext, UploadService::class.java)
        MainApplication.appContext.startService(intent)
        activity?.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
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
            avatarData?.let { fileUploadService.uploadAvatar(fileData = it, isGroup = true) }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Timber.d("Service disconnected")
    }
}
