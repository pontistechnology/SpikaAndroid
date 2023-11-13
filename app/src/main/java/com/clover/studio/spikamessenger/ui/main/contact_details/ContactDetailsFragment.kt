package com.clover.studio.spikamessenger.ui.main.contact_details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.databinding.FragmentContactDetailsBinding
import com.clover.studio.spikamessenger.ui.main.MainActivity
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.chat.startChatScreenActivity
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.Tools.getFilePathUrl
import com.clover.studio.spikamessenger.utils.UserOptions
import com.clover.studio.spikamessenger.utils.dialog.ChooserDialog
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.clover.studio.spikamessenger.utils.helpers.UserOptionsData
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ContactDetailsFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()

    private var user: User? = null
    private var room: ChatRoom? = null

    private var roomId = 0

    private var bindingSetup: FragmentContactDetailsBinding? = null
    private val binding get() = bindingSetup!!
    private var optionList: MutableList<UserOptionsData> = mutableListOf()
    private var pinSwitch: Drawable? = null
    private var muteSwitch: Drawable? = null

    private var navOptionsBuilder: NavOptions? = null

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
                        // Ignore
                    }

                    override fun onSecondOptionClicked() {
                        // Ignore
                    }
                })
            Timber.d("Failed to fetch user data")
        } else {
            user = requireArguments().getParcelable(Const.Navigation.USER_PROFILE)
            roomId = requireArguments().getInt(Const.Navigation.ROOM_ID)
            room = requireArguments().getParcelable(Const.Navigation.ROOM_DATA)
        }

        navOptionsBuilder = Tools.createCustomNavOptions()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentContactDetailsBinding.inflate(inflater, container, false)

        initializeObservers()
        initializeViews()
        return binding.root
    }

    private fun initializeObservers() = with(binding) {
        viewModel.roomWithUsersListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    activity?.let { parent ->
                        it.responseData?.let { roomWithUsers ->
                            startChatScreenActivity(
                                parent,
                                roomWithUsers
                            )
                        }
                    }
                }

                else -> Timber.d("Other error")
            }
        })

        viewModel.checkRoomExistsListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    Timber.d("Room already exists")
                    it.responseData?.data?.room?.roomId?.let { roomId ->
                        viewModel.getRoomWithUsers(
                            roomId
                        )
                    }
                }

                Resource.Status.ERROR -> {
                    Timber.d("Room not found, creating new one")
                    val jsonObject = JsonObject()
                    val userIdsArray = JsonArray()
                    userIdsArray.add(user?.id)

                    jsonObject.addProperty(Const.JsonFields.NAME, user?.formattedDisplayName)
                    jsonObject.addProperty(Const.JsonFields.AVATAR_FILE_ID, user?.avatarFileId)
                    jsonObject.add(Const.JsonFields.USER_IDS, userIdsArray)
                    jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.PRIVATE)

                    viewModel.createNewRoom(jsonObject)
                }

                else -> Timber.d("Other error")
            }
        })

        viewModel.createRoomListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    Timber.d("Room data = ${it.responseData!!.data}")
                    it.responseData.data?.room?.roomId?.let { roomId ->
                        viewModel.getRoomWithUsers(
                            roomId
                        )
                    }
                }

                Resource.Status.ERROR -> Timber.d("Failed to create room")
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
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData != null) {
                        val containsElement =
                            it.responseData.any { blockedUser -> blockedUser.id == user?.id }
                        if (containsElement) {
                            binding.tvBlocked.text = getString(R.string.unblock)
                        } else binding.tvBlocked.text = getString(R.string.block)
                    }
                }

                Resource.Status.ERROR -> Timber.d("Failed to fetch blocked users")
                else -> Timber.d("Other error")
            }
        })
    }

    private fun initializeViews() = with(binding) {
        if (user != null) {
            tvUsername.text = user?.formattedDisplayName
            tvNumber.text = user?.telephoneNumber

            ivAddContact.setOnClickListener {
                ChooserDialog.getInstance(requireContext(),
                    getString(R.string.contacts),
                    null,
                    getString(R.string.copy),
                    getString(R.string.save_contact),
                    object : DialogInteraction {
                        override fun onFirstOptionClicked() {
                            copyNumber(user?.telephoneNumber.toString())
                        }

                        override fun onSecondOptionClicked() {
                            saveContactToPhone(
                                user?.telephoneNumber.toString(),
                                user?.displayName.toString()
                            )
                        }
                    })
            }

            ivChat.setOnClickListener {
                user?.id?.let { id ->
                    run {
                        CoroutineScope(Dispatchers.IO).launch {
                            Timber.d("Checking room id: ${viewModel.checkIfUserInPrivateRoom(id)}")
                            val roomId = viewModel.checkIfUserInPrivateRoom(id)
                            if (roomId != null) {
                                viewModel.getRoomWithUsers(roomId)
                            } else {
                                viewModel.checkIfRoomExists(id)
                            }
                        }
                    }
                }
            }

            Glide.with(this@ContactDetailsFragment)
                .load(user?.avatarFileId?.let { getFilePathUrl(it) })
                .placeholder(
                    ResourcesCompat.getDrawable(
                        requireContext().resources,
                        R.drawable.img_user_avatar,
                        null
                    )
                )
                .error(R.drawable.img_user_avatar)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(profilePicture.ivPickAvatar)

            profilePicture.flProgressScreen.visibility = View.GONE
        }

        ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        tvBlocked.setOnClickListener {
            if (tvBlocked.text.equals(getString(R.string.block))) {
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
                    getString(R.string.unblock_description, user?.formattedDisplayName),
                    getString(R.string.no),
                    getString(R.string.unblock),
                    object : DialogInteraction {
                        override fun onSecondOptionClicked() {
                            user?.id?.let { id -> viewModel.deleteBlockForSpecificUser(id) }
                        }
                    })
            }
        }

        setOptionList()

        val userOptions = UserOptions(requireContext())
        userOptions.setOptions(optionList)
        userOptions.setOptionsListener(object : UserOptions.OptionsListener {
            override fun clickedOption(option: Int, optionName: String) {
                when (optionName) {
                    getString(R.string.notes) -> {
                        goToNotes()
                    }
                }
            }

            override fun switchOption(optionName: String, isSwitched: Boolean) {
                switchPinMuteOptions(optionName, isSwitched)
            }
        })
        binding.flOptionsContainer.addView(userOptions)
    }

    private fun switchPinMuteOptions(optionName: String, isSwitched: Boolean) {
        if (optionName == getString(R.string.pin_chat)) {
            viewModel.handleRoomPin(roomId, isSwitched)
        } else {
            viewModel.handleRoomMute(roomId, isSwitched)
        }
    }

    private fun goToNotes() {
        if (activity is MainActivity) {
            findNavController().navigate(
                R.id.notesFragment,
                bundleOf(Const.Navigation.ROOM_ID to roomId),
                navOptionsBuilder
            )
        } else {
            findNavController().navigate(
                R.id.notesFragment,
                bundleOf(Const.Navigation.ROOM_ID to roomId),
                navOptionsBuilder
            )
        }
    }

    private fun setOptionList() {
        val pinId = if (room?.pinned == true) R.drawable.img_switch else R.drawable.img_switch_left
        val muteId = if (room?.muted == true) R.drawable.img_switch else R.drawable.img_switch_left

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
                additionalText = ""
            ),
            UserOptionsData(
                option = getString(R.string.pin_chat),
                firstDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.img_pin
                ),
                secondDrawable = pinSwitch,
                switchOption = true,
                isSwitched = room?.pinned == true,
                additionalText = ""
            ),
            UserOptionsData(
                option = getString(R.string.mute),
                firstDrawable = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.img_mute
                ),
                secondDrawable = muteSwitch,
                switchOption = true,
                isSwitched = room?.muted == true,
                additionalText = ""
            )
        )
    }

    private fun copyNumber(telephoneNumber: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("", telephoneNumber)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.number_copied), Toast.LENGTH_SHORT)
            .show()

    }

    private fun saveContactToPhone(phoneNumber: String, phoneName: String) {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION)
        intent.type = ContactsContract.RawContacts.CONTENT_TYPE

        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
        intent.putExtra(ContactsContract.Intents.Insert.NAME, phoneName)
        intent.putExtra(
            ContactsContract.Intents.Insert.PHONE_TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        )

        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getBlockedUsersList()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.unregisterSharedPrefsReceiver()
    }
}
