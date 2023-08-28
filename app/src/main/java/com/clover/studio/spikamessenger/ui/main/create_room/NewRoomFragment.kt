package com.clover.studio.spikamessenger.ui.main.create_room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.databinding.FragmentNewRoomBinding
import com.clover.studio.spikamessenger.ui.main.MainFragmentDirections
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.chat.startChatScreenActivity
import com.clover.studio.spikamessenger.ui.main.contacts.ContactsAdapter
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortUsersByLocale
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Arrays
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
    private var newGroupFlag = false

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

        if (viewModel.roomUsers.isNotEmpty()) {
            selectedUsers = viewModel.roomUsers
            selectedContactsAdapter.submitList(selectedUsers)
            handleGroupChat()
            handleNextTextView()
        } else {
            setupAdapter(false)
        }

        initializeObservers()
        initializeViews()

        return binding.root
    }

    private fun initializeViews() = with(binding) {
        // SearchView is immediately acting as if selected
        // Behave like group chat if adding user from ChatDetails.
        if (args?.userIds?.isNotEmpty() == true) handleGroupChat()

        tvNext.setOnClickListener {
            val bundle = bundleOf(Const.Navigation.SELECTED_USERS to selectedUsers)
            if (selectedUsers != viewModel.roomUsers) {
                viewModel.saveSelectedUsers(selectedUsers)
            }

            binding.topAppBar.menu.findItem(R.id.search_menu_icon).collapseActionView()

            if (args?.roomId != null && args?.roomId != 0) {
                updateRoom()
                isRoomUpdate = true
            } else findNavController().navigate(
                R.id.action_newRoomFragment_to_groupInformationFragment,
                bundle
            )
        }

        ivCancel.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        tvNewGroupChat.setOnClickListener {
            handleGroupChat()
        }

        topAppBar.menu.findItem(R.id.create_room_menu_icon).isVisible = false
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search_menu_icon -> {
                    ivCancel.visibility = View.GONE
                    val searchView = menuItem.actionView as SearchView
                    searchView.queryHint = getString(R.string.contact_message_search)
                    searchView.setIconifiedByDefault(false)
                    setupSearchView(searchView)

                    menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                        override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                            return true
                        }

                        override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                            ivCancel.visibility = View.VISIBLE
                            rvContacts.visibility = View.VISIBLE
                            return true
                        }

                    })

                    menuItem.expandActionView()

                    true
                }

                R.id.create_room_menu_icon -> {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToNewRoomFragment())
                    true
                }

                else -> false
            }
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

    private fun handleGroupChat() = with(binding) {
        clSelectedContacts.visibility = View.VISIBLE
        tvSelectedNumber.text = getString(R.string.users_selected, selectedUsers.size)
        tvNewGroupChat.visibility = View.GONE
        tvNext.visibility = View.VISIBLE
        topAppBar.title = getString(R.string.select_members)

        newGroupFlag = true

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

        selectedContactsAdapter.submitList(selectedUsers)
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
            if (it.responseData != null) {
                userList = it.responseData.toMutableList()

                if (newGroupFlag) {
                    userList.removeIf { userData -> userData.user.isBot }
                }

                val users = userList.sortUsersByLocale(requireContext())
                userList = users.toMutableList()
                contactsAdapter.submitList(users)
            }
        }

        viewModel.roomWithUsersListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    hideProgress()
                    if (isRoomUpdate) {
                        findNavController().popBackStack(R.id.chatDetailsFragment, false)
                        isRoomUpdate = false
                    } else {
                        activity?.let { parent ->
                            it.responseData?.let { roomWithUsers ->
                                startChatScreenActivity(
                                    parent,
                                    roomWithUsers
                                )
                            }
                        }
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

                    jsonObject.addProperty(Const.JsonFields.NAME, user?.formattedDisplayName)
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
        viewModel.getRoomWithUsers(chatRoom.roomId)
    }

    private fun setupSearchView(searchView: SearchView) {
        // SearchView is immediately acting as if selected
        searchView.setIconifiedByDefault(false)
        searchView.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    if (::userList.isInitialized) {
                        for (user in userList) {
                            if (user.phoneUser?.name?.lowercase()?.contains(
                                    query,
                                    ignoreCase = true
                                ) ?: user.user.formattedDisplayName.lowercase()
                                    .contains(query, ignoreCase = true)
                            ) {
                                filteredList.add(user)
                            }
                        }
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
                if (!query.isNullOrEmpty()) {
                    if (::userList.isInitialized) {
                        for (user in userList) {
                            if (user.phoneUser?.name?.lowercase()?.contains(
                                    query,
                                    ignoreCase = true
                                ) ?: user.user.formattedDisplayName.lowercase()
                                    .contains(query, ignoreCase = true)
                            ) {
                                filteredList.add(user)
                            }
                        }
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

        searchView.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus) {
                    hideKeyboard(view)
                    binding.ivCancel.visibility = View.VISIBLE
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
