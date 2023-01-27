package com.clover.studio.exampleapp.ui.main.contact_details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.databinding.FragmentContactDetailsBinding
import com.clover.studio.exampleapp.ui.main.*
import com.clover.studio.exampleapp.ui.main.chat.startChatScreenActivity
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools.getFilePathUrl
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import timber.log.Timber

class ContactDetailsFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()

    private var user: User? = null

    private var roomId = 0

    private var bindingSetup: FragmentContactDetailsBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireArguments().getParcelable<User>(Const.Navigation.USER_PROFILE) == null) {
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
            user = requireArguments().getParcelable(Const.Navigation.USER_PROFILE)
            roomId = requireArguments().getInt(Const.Navigation.ROOM_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentContactDetailsBinding.inflate(inflater, container, false)

        initializeViews()
        initializeObservers()
        return binding.root
    }

    private fun initializeObservers() {
        viewModel.getRoomByIdLiveData(roomId).observe(viewLifecycleOwner) { room ->
            if (room != null) {
                // Set room muted or not muted on switch
                binding.swMute.isChecked = room.muted

                // Set room pinned or not pinned on switch
                binding.swPinChat.isChecked = room.pinned
            }
        }

        viewModel.roomWithUsersListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is RoomWithUsersFetched -> {
                    Timber.d("Room with users = ${it.roomWithUsers}")
                    val gson = Gson()
                    val roomData = gson.toJson(it.roomWithUsers)
                    Timber.d("ROOM data: = ${it.roomWithUsers}")
                    activity?.let { parent -> startChatScreenActivity(parent, roomData) }
                }
                else -> Timber.d("Other error")
            }
        })

        viewModel.checkRoomExistsListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is RoomExists -> {
                    Timber.d("Room already exists")
                    viewModel.getRoomWithUsers(it.roomData.roomId)
                }
                is RoomNotFound -> {
                    Timber.d("Room not found, creating new one")
                    val jsonObject = JsonObject()

//                    val avatarUrlArray = JsonArray()
//                    avatarUrlArray.add("fakeUrl")
//
                    val userIdsArray = JsonArray()
                    userIdsArray.add(user?.id)
//                    userIdsArray.add(5)
//
//                    val adminUserIds = JsonArray()
//                    adminUserIds.add(3)

                    jsonObject.addProperty(Const.JsonFields.NAME, user?.displayName)
                    jsonObject.addProperty(Const.JsonFields.AVATAR_FILE_ID, user?.avatarFileId)
                    jsonObject.add(Const.JsonFields.USER_IDS, userIdsArray)
//                    jsonObject.add(Const.JsonFields.ADMIN_USER_IDS, adminUserIds)
                    jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.PRIVATE)

                    viewModel.createNewRoom(jsonObject)
                }
                else -> Timber.d("Other error")
            }
        })

        viewModel.createRoomListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is RoomCreated -> {
                    val gson = Gson()
                    val roomData = gson.toJson(it.roomData)
                    Timber.d("Room data = $roomData")
                    viewModel.getRoomWithUsers(it.roomData.roomId)
                }
                is RoomCreateFailed -> Timber.d("Failed to create room")
                else -> Timber.d("Other error")
            }
        })

        viewModel.blockedUserListListener().observe(viewLifecycleOwner) {
            if (it?.isNotEmpty() == true) {
                viewModel.fetchBlockedUsersLocally(it)
            } else {
                binding.tvBlocked.text = getString(R.string.block)
            }
        }

        viewModel.blockedListListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is BlockedUsersFetched -> {
                    if (it.users.isNotEmpty()) {
                        val containsElement =
                            it.users.any { blockedUser -> blockedUser.id == user?.id }
                        if (containsElement) {
                            binding.tvBlocked.text = getString(R.string.unblock)
                        } else binding.tvBlocked.text = getString(R.string.block)
                    }
                }
                BlockedUsersFetchFailed -> Timber.d("Failed to fetch blocked users")
                else -> Timber.d("Other error")
            }
        })
    }

    private fun initializeViews() {
        if (user != null) {
            binding.tvUsername.text = user?.displayName
            binding.tvPageName.text = user?.displayName

            binding.ivChat.setOnClickListener {
                user?.id?.let { id -> viewModel.checkIfRoomExists(id) }
            }

            Glide.with(this).load(user?.avatarFileId?.let { getFilePathUrl(it) })
                .placeholder(
                    ResourcesCompat.getDrawable(
                        requireContext().resources,
                        R.drawable.img_user_placeholder,
                        null
                    )
                )
                .into(binding.ivPickAvatar)
            binding.clProgressScreen.visibility = View.GONE
        }

        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.tvBlocked.setOnClickListener {
            if (binding.tvBlocked.text.equals(getString(R.string.block))) {
                DialogError.getInstance(requireContext(),
                    getString(R.string.block_user),
                    getString(R.string.block_user_description),
                    getString(R.string.no),
                    getString(R.string.block),
                    object : DialogInteraction {
                        override fun onSecondOptionClicked() {
                            user?.id?.let { id -> viewModel.blockUser(id) }
                        }
                    })
            } else {
                DialogError.getInstance(requireContext(),
                    getString(R.string.unblock_user),
                    getString(R.string.unblock_description, user?.displayName),
                    getString(R.string.no),
                    getString(R.string.unblock),
                    object : DialogInteraction {
                        override fun onSecondOptionClicked() {
                            user?.id?.let { id -> viewModel.deleteBlockForSpecificUser(id) }
                        }
                    })
            }
        }

        binding.swMute.setOnCheckedChangeListener(multiListener)

        binding.swPinChat.setOnCheckedChangeListener(multiListener)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getBlockedUsersList()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unregisterSharedPrefsReceiver()
    }

    private val multiListener: CompoundButton.OnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            when (buttonView.id) {
                binding.swPinChat.id -> {
                    if (buttonView.isPressed) {
                        if (isChecked) {
                            pinRoom()
                        } else {
                            unpinRoom()
                        }
                    }
                }
                binding.swMute.id -> {
                    if (buttonView.isPressed) {
                        if (isChecked) {
                            muteRoom()
                        } else {
                            unmuteRoom()
                        }
                    }
                }
            }
        }

    private fun muteRoom() {
        roomId.let { viewModel.muteRoom(it) }
    }

    private fun unmuteRoom() {
        roomId.let { viewModel.unmuteRoom(it) }
    }

    private fun pinRoom() {
        roomId.let { viewModel.pinRoom(it) }
    }

    private fun unpinRoom() {
        roomId.let { viewModel.unpinRoom(it) }
    }
}