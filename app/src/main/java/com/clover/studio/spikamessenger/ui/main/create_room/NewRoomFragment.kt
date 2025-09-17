package com.clover.studio.spikamessenger.ui.main.create_room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.ChatRoom
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import com.clover.studio.spikamessenger.databinding.FragmentNewRoomBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.chat.startChatScreenActivity
import com.clover.studio.spikamessenger.ui.main.contacts.UsersGroupsAdapter
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortChats
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class NewRoomFragment : BaseFragment() {
    private var args: NewRoomFragmentArgs? = null
    private val viewModel: MainViewModel by activityViewModels()
    private var contactsAdapter: UsersGroupsAdapter? = null
    private var selectedContactsAdapter: UsersGroupsSelectedAdapter? = null
    private var userList: MutableList<PrivateGroupChats> = mutableListOf()
    private var selectedUsers: MutableList<PrivateGroupChats> = ArrayList()
    private var filteredList: MutableList<PrivateGroupChats> = ArrayList()
    private var user: PrivateGroupChats? = null
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

        if (viewModel.roomUsers.isNotEmpty() && args?.userIds == null) {
            selectedUsers = viewModel.roomUsers
            selectedContactsAdapter?.submitList(selectedUsers)
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
        // Behave like group chat if adding user from ChatDetails.
        if (args?.userIds?.isNotEmpty() == true) handleGroupChat()

        fabNext.setOnClickListener {
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

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search_menu_icon -> {
                    ivCancel.visibility = View.GONE
                    val searchView = menuItem.actionView as SearchView
                    searchView.let {
                        Tools.setUpSearchBar(
                            requireContext(),
                            it,
                            getString(R.string.search_contacts)
                        )
                    }

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

                else -> false
            }
        }
    }

    private fun updateRoom() {
        val jsonObject = JsonObject()
        val userIds = JsonArray()

        for (data in selectedUsers) {
            userIds.add(data.userId)
        }

        if (userIds.size() > 0) {
            jsonObject.addProperty(Const.JsonFields.ACTION, Const.JsonFields.ADD_GROUP_USERS)
            jsonObject.add(Const.JsonFields.USER_IDS, userIds)
        }

        args?.roomId?.let { viewModel.updateRoom(jsonObject, it) }
    }

    private fun handleGroupChat() = with(binding) {
        clSelectedContacts.visibility = View.VISIBLE
        tvSelectedNumber.text = getString(R.string.users_selected, selectedUsers.size)
        tvNewGroupChat.visibility = View.GONE
        fabNext.visibility = View.GONE
        topAppBar.title = getString(R.string.select_members)

        newGroupFlag = true

        initializeObservers()
        setupAdapter(true)
    }

    private fun setupAdapter(isGroupCreation: Boolean) = with(binding) {
        // Check if there are some userIds already in Room if we are adding Room users
        // This is only for adding users to Room
        val userIdsInRoom = args?.userIds?.map { it }

        contactsAdapter =
            UsersGroupsAdapter(
                requireContext(),
                isGroupCreation,
                userIdsInRoom,
                isForward = false
            ) {
                if (tvNewGroupChat.visibility == View.GONE) {
                    if (selectedUsers.any { user -> user.userId == it.userId }) {
                        selectedUsers.remove(selectedUsers.first { user -> user.userId == it.userId })
                        tvSelectedNumber.text =
                            getString(R.string.users_selected, selectedUsers.size)
                    } else {
                        selectedUsers.add(it)
                        tvSelectedNumber.text =
                            getString(R.string.users_selected, selectedUsers.size)
                    }

                    selectedContactsAdapter?.notifyDataSetChanged()

                    handleNextTextView()
                    handleSelectedUserList(it)
                } else {
                    user = it
                    showProgress(false)
                    it.userId.let { id ->
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

        setUpSelectedContactsAdapter()

        selectedContactsAdapter?.submitList(selectedUsers)
        binding.rvSelected.adapter = selectedContactsAdapter
        binding.rvSelected.layoutManager =
            LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
    }

    private fun setUpSelectedContactsAdapter() {
        selectedContactsAdapter = UsersGroupsSelectedAdapter(requireContext()) {
            if (selectedUsers.any { user -> user.userId == it.userId }) {
                selectedUsers.remove(it)
                selectedContactsAdapter?.submitList(selectedUsers)
                binding.tvSelectedNumber.text =
                    getString(R.string.users_selected, selectedUsers.size)
                selectedContactsAdapter?.notifyDataSetChanged()
            }

            handleNextTextView()
            handleSelectedUserList(it)
        }
    }

    private fun handleSelectedUserList(userItem: PrivateGroupChats) {
        userList.find { it.userId == userItem.userId }?.apply {
            selected = !selected
        }

        contactsAdapter?.submitList(userList)
        contactsAdapter?.notifyDataSetChanged()
    }

    private fun handleNextTextView() {
        binding.fabNext.visibility = if (selectedUsers.size > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun initializeObservers() {
        viewModel.getUserAndPhoneUserLiveData(localId).observe(viewLifecycleOwner) {
            if (it.responseData != null) {
                userList = Tools.transformPrivateList(requireContext(), it.responseData)

                if (newGroupFlag) {
                    userList.removeIf { userData -> userData.isBot }
                }

                val users = userList.sortChats(requireContext())
                users.forEach { user ->
                    val isSelected = selectedUsers.any { selectedUser ->
                        user.userId == selectedUser.userId
                    }
                    user.selected = isSelected
                }

                userList = users.toMutableList()
                contactsAdapter?.submitList(userList)
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
                    userIdsArray.add(user?.userId)

                    jsonObject.addProperty(Const.JsonFields.NAME, user?.userName)
                    jsonObject.addProperty(Const.JsonFields.AVATAR_FILE_ID, user?.avatarId)
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
                makeQuery(query)
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                makeQuery(query)
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

    fun makeQuery(query: String?) {
        if (query != null) {
            for (user in userList) {
                if (user.userName?.lowercase()
                        ?.contains(query, ignoreCase = true) == true
                    || user.userPhoneName?.lowercase()?.contains(query, ignoreCase = true) == true
                ) {
                    filteredList.add(user)
                }
            }
            val users = filteredList.sortChats(requireContext())
            contactsAdapter?.submitList(null)
            contactsAdapter?.submitList(ArrayList(users)) {
                binding.rvContacts.scrollToPosition(0)
            }
            filteredList.clear()
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
                // Ignore
            })
    }
}
