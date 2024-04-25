package com.clover.studio.spikamessenger.ui.main.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import com.clover.studio.spikamessenger.databinding.FragmentContactsBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.chat.startChatScreenActivity
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortChats
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ContactsFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var contactsAdapter: UsersGroupsAdapter
    private var userList: MutableList<PrivateGroupChats> = mutableListOf()
    private var filteredList: MutableList<PrivateGroupChats> = ArrayList()
    private var selectedUser: PrivateGroupChats? = null

    private var bindingSetup: FragmentContactsBinding? = null
    private val binding get() = bindingSetup!!

    private var navOptionsBuilder: NavOptions? = null

    private var localId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentContactsBinding.inflate(inflater, container, false)
        navOptionsBuilder = Tools.createCustomNavOptions()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        localId = viewModel.getLocalUserId()!!
        setupAdapter()
        setupSwipeToRefresh()
        initializeViews()
        initializeObservers()
    }

    private fun setupSwipeToRefresh() {
        binding.srlRefreshContacts.setOnRefreshListener {
            viewModel.syncContacts()
        }
        binding.srlRefreshContacts.isEnabled = true
    }

    private fun initializeViews() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search_menu_icon -> {
                    val searchView = menuItem.actionView as SearchView
                    searchView.let {
                        Tools.setUpSearchBar(
                            requireContext(),
                            it,
                            getString(R.string.search_contacts)
                        )
                    }
                    setupSearchView(searchView)

                    menuItem.expandActionView()

                    true
                }

                else -> false
            }
        }
    }

    private fun initializeObservers() {
        viewModel.usersListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> Timber.d("Users fetched")
                Resource.Status.ERROR -> Timber.d("Users error")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getUserAndPhoneUserLiveData(localId).observe(viewLifecycleOwner) {
            if (it.responseData != null) {
                userList.clear()
                userList = Tools.transformPrivateList(requireContext(), it.responseData)

                contactsAdapter.submitList(userList)
            }
        }

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
                    userIdsArray.add(selectedUser?.userId)

                    jsonObject.addProperty(Const.JsonFields.NAME, selectedUser?.userName)
                    jsonObject.addProperty(
                        Const.JsonFields.AVATAR_FILE_ID,
                        selectedUser?.avatarId
                    )
                    jsonObject.add(Const.JsonFields.USER_IDS, userIdsArray)
                    jsonObject.addProperty(Const.JsonFields.TYPE, Const.JsonFields.PRIVATE)

                    Timber.d("Json object: $jsonObject")

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

        viewModel.contactSyncListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    Timber.d("Contact sync success")
                    binding.srlRefreshContacts.isRefreshing = false
                }

                Resource.Status.ERROR -> {
                    Timber.d("Contact sync error")
                    Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_SHORT).show()
                    binding.srlRefreshContacts.isRefreshing = false
                }

                else -> {
                    Timber.d("Contact sync else")
                    Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_SHORT).show()
                    binding.srlRefreshContacts.isRefreshing = false
                }
            }
        })
    }

    private fun setupAdapter() {
        contactsAdapter = UsersGroupsAdapter(requireContext(), false, null, isForward = false) {
            selectedUser = it
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

        val linearLayoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvContacts.apply {
            adapter = contactsAdapter
            itemAnimator = null
            layoutManager = linearLayoutManager
        }
    }

    private fun setupSearchView(searchView: SearchView) {
        // SearchView is immediately acting as if selected
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
            contactsAdapter.submitList(null)
            contactsAdapter.submitList(ArrayList(users))
            filteredList.clear()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.topAppBar.menu.findItem(R.id.search_menu_icon).collapseActionView()
    }
}
