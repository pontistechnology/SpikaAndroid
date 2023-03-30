package com.clover.studio.exampleapp.ui.main.create_room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.ChatRoom
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.entity.UserAndPhoneUser
import com.clover.studio.exampleapp.databinding.FragmentNewRoomBinding
import com.clover.studio.exampleapp.ui.main.*
import com.clover.studio.exampleapp.ui.main.chat.startChatScreenActivity
import com.clover.studio.exampleapp.ui.main.contacts.ContactsAdapter
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.clover.studio.exampleapp.utils.helpers.Extensions.sortUsersByLocale
import com.clover.studio.exampleapp.utils.helpers.GsonProvider
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.streams.toList

class NewRoomFragment : BaseFragment() {
    private var args: NewRoomFragmentArgs? = null
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var selectedContactsAdapter: SelectedContactsAdapter
    private lateinit var userList: MutableList<UserAndPhoneUser>
    private var selectedUsers: MutableList<UserAndPhoneUser> = ArrayList()
    private var filteredList: MutableList<UserAndPhoneUser> = ArrayList()
    private var user: User? = null
    private var isRoomUpdate = false

    private var bindingSetup: FragmentNewRoomBinding? = null

    private val binding get() = bindingSetup!!

    private var localId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentNewRoomBinding.inflate(inflater, container, false)

        val bundle = arguments
        args = if (bundle != null)
            NewRoomFragmentArgs.fromBundle(bundle)
        else null

        localId = viewModel.getLocalUserId()!!
        initializeObservers()
        setupAdapter(false)
        initializeViews()
        setupSearchView()

        return binding.root
    }

    private fun initializeViews() {
        // SearchView is immediately acting as if selected

        // Behave like group chat if adding user from ChatDetails.
        if (args?.userIds?.isNotEmpty() == true) handleGroupChat()

        binding.svContactsSearch.setIconifiedByDefault(false)

        binding.tvNext.setOnClickListener {
            val bundle = bundleOf(Const.Navigation.SELECTED_USERS to selectedUsers)

            if (args?.roomId != null && args?.roomId != 0) {
                updateRoom()
                isRoomUpdate = true
            } else findNavController().navigate(
                R.id.action_newRoomFragment_to_groupInformationFragment,
                bundle
            )
        }

        binding.ivCancel.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.tvNewGroupChat.setOnClickListener {
            handleGroupChat()
        }
    }

    private fun updateRoom() {
        val jsonObject = JsonObject()
        val userIds = JsonArray()

        for (data in selectedUsers) {
            userIds.add(data.user.id)
        }

        val userIdsInRoom = args?.userIds?.let { Arrays.stream(it).boxed().toList() }
        if (userIdsInRoom?.isNotEmpty() == true) {
            for (id in userIdsInRoom) {
                userIds.add(id)
            }
        }

        if (userIds.size() > 0)
            jsonObject.add(Const.JsonFields.USER_IDS, userIds)

        args?.roomId?.let { viewModel.updateRoom(jsonObject, it, 0) }
    }

    private fun handleGroupChat() {
        // Clear data if old data is still present
        selectedUsers.clear()
        binding.clSelectedContacts.visibility = View.VISIBLE
        binding.tvSelectedNumber.text = getString(R.string.users_selected, selectedUsers.size)
        binding.tvNewGroupChat.visibility = View.GONE
        binding.tvNext.visibility = View.VISIBLE
        binding.tvTitle.text = getString(R.string.select_members)

        setupAdapter(true)
        initializeObservers()
    }

    private fun setupAdapter(isGroupCreation: Boolean) {
        // Contacts Adapter

        // Check if there are some userIds already in Room if we are adding Room users
        // This is only for adding users to Room
        val userIdsInRoom = args?.userIds?.let { Arrays.stream(it).boxed().toList() }

        contactsAdapter = ContactsAdapter(requireContext(), isGroupCreation, userIdsInRoom) {
            if (binding.tvNewGroupChat.visibility == View.GONE) {
                if (selectedUsers.contains(it)) {
                    selectedUsers.remove(it)
                    binding.tvSelectedNumber.text =
                        getString(R.string.users_selected, selectedUsers.size)
                } else {
                    selectedUsers.add(it)
                    binding.tvSelectedNumber.text =
                        getString(R.string.users_selected, selectedUsers.size)
                }
                selectedContactsAdapter.submitList(selectedUsers)
                selectedContactsAdapter.notifyDataSetChanged()

                handleNextTextView()
                handleSelectedUserList(it)
            } else {
                user = it.user
                showProgress(false)
                it.user.id.let { id ->
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
        }

        binding.rvContacts.adapter = contactsAdapter
        binding.rvContacts.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        // Contacts Selected Adapter
        selectedContactsAdapter = SelectedContactsAdapter(requireContext()) {
            if (selectedUsers.contains(it)) {
                selectedUsers.remove(it)
                selectedContactsAdapter.submitList(selectedUsers)
                binding.tvSelectedNumber.text =
                    getString(R.string.users_selected, selectedUsers.size)
                selectedContactsAdapter.notifyDataSetChanged()
            }

            handleNextTextView()
            handleSelectedUserList(it)
        }

        binding.rvSelected.adapter = selectedContactsAdapter
        binding.rvSelected.layoutManager =
            LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
    }

    private fun handleSelectedUserList(userItem: UserAndPhoneUser) {
        for (user in userList) {
            if (user == userItem) {
                user.user.selected = !user.user.selected
            }
        }

        contactsAdapter.submitList(userList)
        contactsAdapter.notifyDataSetChanged()
    }

    private fun handleNextTextView() {
        if (selectedUsers.size > 0) {
            binding.tvNext.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.primary_color
                )
            )
            binding.tvNext.isClickable = true
        } else {
            binding.tvNext.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_tertiary
                )
            )
            binding.tvNext.isClickable = false
        }
    }

    private fun initializeObservers() {
        viewModel.getUserAndPhoneUser(localId).observe(viewLifecycleOwner) {
            // TODO @Ivana handle this null check
            if (it.responseData != null) {
                userList = it.responseData.toMutableList()
                val users = userList.sortUsersByLocale(requireContext())
                userList = users.toMutableList()
                contactsAdapter.submitList(users)
            }
        }

        viewModel.roomWithUsersListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    hideProgress()
                    val gson = GsonProvider.gson
                    val roomData = gson.toJson(it.responseData)
                    if (isRoomUpdate) {
                        findNavController().popBackStack(R.id.chatDetailsFragment, false)
                        isRoomUpdate = false
                    } else {
                        activity?.let { parent -> startChatScreenActivity(parent, roomData) }
                        findNavController().popBackStack(R.id.mainFragment, false)
                    }
                }
                else -> {
                    hideProgress()
                    showRoomCreationError(getString(R.string.room_local_fetch_error))
                    Timber.d("Other error")
                }
            }
        })

        viewModel.checkRoomExistsListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    Timber.d("Room already exists")
                    viewModel.getRoomWithUsers(it.responseData?.data?.room?.roomId!!)
                }
                Resource.Status.ERROR -> {
                    Timber.d("Room not found, creating new one")
                    val jsonObject = JsonObject()

                    val userIdsArray = JsonArray()
                    userIdsArray.add(user?.id)

                    jsonObject.addProperty(Const.JsonFields.NAME, user?.displayName)
                    jsonObject.addProperty(Const.JsonFields.AVATAR_FILE_ID, user?.avatarFileId)
                    jsonObject.add(Const.JsonFields.USER_IDS, userIdsArray)
                    jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.PRIVATE)

                    viewModel.createNewRoom(jsonObject)
                }
                else -> {
                    hideProgress()
                    showRoomCreationError(getString(R.string.error_room_exists))
                    Timber.d("Other error")
                }
            }
        })

        viewModel.createRoomListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    handleRoomData(it.responseData?.data?.room!!)
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
    }

    private fun handleRoomData(chatRoom: ChatRoom) {
        hideProgress()
        val gson = GsonProvider.gson
        val roomData = gson.toJson(chatRoom)
        Timber.d("Room data = $roomData")
        viewModel.getRoomWithUsers(chatRoom.roomId)
    }

    private fun setupSearchView() {
        // SearchView is immediately acting as if selected
        binding.svContactsSearch.setIconifiedByDefault(false)
        binding.svContactsSearch.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    Timber.d("Query: $query")
                    if (::userList.isInitialized) {
                        for (user in userList) {
                            if ((user.phoneUser?.name?.lowercase()?.contains(
                                    query,
                                    ignoreCase = true
                                ) ?: user.user.displayName?.lowercase()
                                    ?.contains(query, ignoreCase = true)) == true
                            ) {
                                filteredList.add(user)
                            }
                        }
                        Timber.d("Filtered List: $filteredList")
                        val users = filteredList.sortUsersByLocale(requireContext())
                        contactsAdapter.submitList(ArrayList(users)) {
                            binding.rvContacts.scrollToPosition(0)
                        }
                        filteredList.clear()
                    }
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    Timber.d("Query: $query")
                    if (::userList.isInitialized) {
                        for (user in userList) {
                            if ((user.phoneUser?.name?.lowercase()?.contains(
                                    query,
                                    ignoreCase = true
                                ) ?: user.user.displayName?.lowercase()
                                    ?.contains(query, ignoreCase = true)) == true
                            ) {
                                filteredList.add(user)
                            }
                        }
                        Timber.d("Filtered List: $filteredList")
                        val users = filteredList.sortUsersByLocale(requireContext())
                        contactsAdapter.submitList(ArrayList(users)) {
                            binding.rvContacts.scrollToPosition(0)
                        }
                        filteredList.clear()
                    }
                }
                return true
            }
        })

        binding.svContactsSearch.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        arguments?.clear()
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