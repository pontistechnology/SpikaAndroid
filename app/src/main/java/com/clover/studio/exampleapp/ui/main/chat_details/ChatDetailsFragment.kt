package com.clover.studio.exampleapp.ui.main.chat_details

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.data.repositories.SharedPreferencesRepository
import com.clover.studio.exampleapp.databinding.FragmentChatDetailsBinding
import com.clover.studio.exampleapp.ui.main.chat.ChatViewModel
import com.clover.studio.exampleapp.ui.main.chat.RoomWithUsersFailed
import com.clover.studio.exampleapp.ui.main.chat.RoomWithUsersFetched
import com.clover.studio.exampleapp.utils.*
import com.clover.studio.exampleapp.utils.dialog.ChooserDialog
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.dialog.DialogInteraction
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private var progress: Long = 1L
    private var avatarPath: String? = null
    private var roomId: Int? = null
    private var isAdmin = false

    private var bindingSetup: FragmentChatDetailsBinding? = null
    private val binding get() = bindingSetup!!

    private val chooseImageContract =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                val bitmap =
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), it)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).into(binding.ivPickAvatar)
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
                    Tools.handleSamplingAndRotationBitmap(requireActivity(), currentPhotoLocation)
                val bitmapUri = Tools.convertBitmapToUri(requireActivity(), bitmap!!)

                Glide.with(this).load(bitmap).into(binding.ivPickAvatar)
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
        roomId = args.roomId
        isAdmin = args.isAdmin
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentChatDetailsBinding.inflate(inflater, container, false)

        initializeObservers()
        handleUserStatusViews(isAdmin)
        setupAdapter(isAdmin)

        return binding.root
    }

    private fun handleUserStatusViews(isAdmin: Boolean) {
        if (!isAdmin) {
            binding.tvGroupName.isClickable = false
            binding.tvDone.isFocusable = false

            binding.cvAvatar.isClickable = false
            binding.cvAvatar.isFocusable = false

            binding.ivAddMember.visibility = View.INVISIBLE
        }
    }

    private fun initializeViews(roomWithUsers: RoomWithUsers) {
        if (Const.JsonFields.PRIVATE == roomWithUsers.room.type) {
            roomWithUsers.users.forEach { roomUser ->
                if (viewModel.getLocalUserId().toString() != roomUser.id.toString()) {
                    binding.tvChatName.text = roomUser.displayName
                    Glide.with(this)
                        .load(roomUser.avatarUrl?.let { Tools.getAvatarUrl(it) })
                        .into(binding.ivPickAvatar)
                    Glide.with(this)
                        .load(roomUser.avatarUrl?.let { Tools.getAvatarUrl(it) })
                        .into(binding.ivUserImage)
                }
            }
        } else {
            binding.tvChatName.text = roomWithUsers.room.name
            Glide.with(this).load(roomWithUsers.room.avatarUrl?.let { Tools.getAvatarUrl(it) })
                .into(binding.ivPickAvatar)
            Glide.with(this).load(roomWithUsers.room.avatarUrl?.let { Tools.getAvatarUrl(it) })
                .into(binding.ivUserImage)
        }

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

        binding.tvTitle.text = roomWithUsers.room.type
        binding.tvGroupName.text = roomWithUsers.room.name

        binding.tvGroupName.setOnClickListener {
            binding.etEnterGroupName.visibility = View.VISIBLE
            binding.tvDone.visibility = View.VISIBLE
            binding.tvGroupName.visibility = View.INVISIBLE
            binding.ivCallUser.visibility = View.GONE
            binding.ivVideoCall.visibility = View.GONE
        }

        binding.tvDone.setOnClickListener {
            val roomName = binding.etEnterGroupName.text.toString()

//            val adminIds: MutableList<Int> = ArrayList()

            val jsonObject = JsonObject()
            if (roomName.isNotEmpty()) {
                jsonObject.addProperty(Const.JsonFields.NAME, roomName)
            }

            if (avatarPath?.isNotEmpty() == true) {
                jsonObject.addProperty(Const.JsonFields.AVATAR_URL, avatarPath)
            }

            viewModel.updateRoom(jsonObject, roomWithUsers.room.roomId, 0)
        }

        binding.cvAvatar.setOnClickListener {
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

        binding.tvMembersNumber.text =
            getString(R.string.number_of_members, roomWithUsers.users.size)

        binding.ivBack.setOnClickListener {
            val action =
                ChatDetailsFragmentDirections.actionChatDetailsFragmentToChatMessagesFragment2()
            findNavController().navigate(action)

        }
    }

    private fun initializeObservers() {
        roomId?.let {
            viewModel.getRoomAndUsers(it).observe(viewLifecycleOwner) { roomWithUsers ->
                if (roomWithUsers != null) {
                    initializeViews(roomWithUsers)
                    updateRoomUserList(roomWithUsers)
                }
            }
        }

        viewModel.roomWithUsersListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is RoomWithUsersFetched -> {
                    initializeViews(it.roomWithUsers)
                    handleUserStatusViews(isAdmin)
                }
                RoomWithUsersFailed -> Timber.d("Failed to fetch chat room")
                else -> Timber.d("Other error")
            }
        })
    }

    private fun setupAdapter(isAdmin: Boolean) {
        adapter = ChatDetailsAdapter(
            requireContext(),
            isAdmin,
            object : ChatDetailsAdapter.DetailsAdapterListener {
                override fun onItemClicked(user: User) {
                    Timber.d("User item clicked = $user")
                    user.displayName?.let {
                        ChooserDialog.getInstance(requireContext(),
                            it,
                            null,
                            getString(R.string.info),
                            getString(R.string.make_group_admin),
                            object : DialogInteraction {
                                override fun onFirstOptionClicked() {
                                    // TODO("Not yet implemented")
                                }

                                override fun onSecondOptionClicked() {
                                    // TODO("Not yet implemented")
                                }

                            })
                    }
                }

                override fun onViewClicked(position: Int, user: User) {
                    Timber.d("Remove user item clicked = $user, $position")
                    Timber.d("${roomUsers.size}")
                    roomUsers.remove(user)
                    updateRoomUsers(user.id)
                    val modifiedList =
                        roomUsers.sortedBy { roomUser -> roomUser.isAdmin }.reversed()
                    adapter.submitList(modifiedList)
                    adapter.notifyDataSetChanged()
                }
            })

        binding.rvGroupMembers.adapter = adapter
        binding.rvGroupMembers.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
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
            val modifiedList = roomUsers.sortedBy { user -> user.isAdmin }.reversed()
            adapter.submitList(modifiedList)
            adapter.notifyDataSetChanged()
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
                    Const.JsonFields.IMAGE,
                    Const.JsonFields.AVATAR,
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

                        override fun fileUploadVerified(path: String, thumbId: Long, fileId: Long) {
                            Timber.d("Upload verified")
                            requireActivity().runOnUiThread {
                                binding.clProgressScreen.visibility = View.GONE
                            }

                            avatarPath = path
                        }

                    })
            }
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
            "com.clover.studio.exampleapp.fileprovider",
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

        if (userIds.size() > 0)
            jsonObject.add(Const.JsonFields.USER_IDS, userIds)

        roomId?.let { viewModel.updateRoom(jsonObject, it, idToRemove) }
    }
}