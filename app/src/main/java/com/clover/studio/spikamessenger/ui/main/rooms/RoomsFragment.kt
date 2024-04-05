package com.clover.studio.spikamessenger.ui.main.rooms

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.RoomWithMessage
import com.clover.studio.spikamessenger.databinding.FragmentRoomsBinding
import com.clover.studio.spikamessenger.ui.main.MainFragmentDirections
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.chat.startChatScreenActivity
import com.clover.studio.spikamessenger.ui.main.rooms.search.SearchAdapter
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.helpers.ColorHelper
import com.clover.studio.spikamessenger.utils.helpers.Resource
import kotlinx.coroutines.launch
import timber.log.Timber

class RoomsFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var searchAdapter: SearchAdapter
    private var roomList: MutableList<RoomWithMessage> = mutableListOf()
    private var filteredList: MutableList<RoomWithMessage> = mutableListOf()
    private var sortedList: MutableList<RoomWithMessage> = mutableListOf()
    private var bindingSetup: FragmentRoomsBinding? = null
    private var searchMessageId = 0

    private var userSearching = false
    private var searchView: SearchView? = null
    private var searchQuery: String = ""

    private var primaryColor = ColorStateList.valueOf(0)
    private var fourthAdditionalColor = ColorStateList.valueOf(0)

    private val binding get() = bindingSetup!!

    private var navOptionsBuilder: NavOptions? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentRoomsBinding.inflate(inflater, container, false)
        navOptionsBuilder = Tools.createCustomNavOptions()

        primaryColor = ColorStateList.valueOf(ColorHelper.getPrimaryColor(requireContext()))
        fourthAdditionalColor =
            ColorStateList.valueOf(ColorHelper.getFourthAdditionalColor(requireContext()))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeObservers()
        initializeViews()
        setupAdapter()
    }

    private fun initializeObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isRoomRefreshing.collect {
                    if (it && userSearching) binding.topAppBar.collapseActionView()
                }
            }
        }

        viewModel.getChatRoomsWithLatestMessage().observe(viewLifecycleOwner) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (!it.responseData.isNullOrEmpty()) {
                        binding.tvNoChats.visibility = View.GONE

                        roomList = it.responseData.toMutableList()

                        val nonEmptyRoomList = it.responseData.filter { roomData ->
                            Const.JsonFields.GROUP == roomData.roomWithUsers.room.type || roomData.message != null
                        }

                        val pinnedRooms =
                            roomList.filter { roomItem -> roomItem.roomWithUsers.room.pinned }
                                .sortedBy { pinnedRoom -> pinnedRoom.message?.createdAt }.reversed()

                        try {
                            sortedList =
                                nonEmptyRoomList.sortedWith(compareBy(nullsFirst()) { roomItem ->
                                    if (roomItem.message != null) {
                                        roomItem.message.createdAt
                                    } else null
                                }).reversed().toMutableList()
                        } catch (ex: Exception) {
                            Tools.checkError(ex)
                        }

                        if (sortedList.isEmpty()) {
                            sortedList = it.responseData.toMutableList()
                        }

                        // Calling .toSet() here caused a crash in the app, so don't add it.
                        sortedList = (pinnedRooms + (sortedList - pinnedRooms)).toMutableList()

                        if (!userSearching) {
                            roomsAdapter.submitList(sortedList)
                        }
                    } else {
                        binding.tvNoChats.visibility = View.VISIBLE
                    }
                }

                Resource.Status.LOADING -> Timber.d("Rooms loading")
                Resource.Status.ERROR -> {
                    binding.tvNoChats.visibility = View.VISIBLE
                    Timber.d("Rooms Error")
                }

                else -> Timber.d("Rooms unknown state")
            }
        }

        viewModel.roomWithUsersListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (searchMessageId != 0) {
                        it.responseData?.let { roomWithUsers ->
                            startChatScreenActivity(
                                requireActivity(),
                                roomWithUsers,
                                searchMessageId
                            )
                        }
                        searchMessageId = 0
                    } else {
                        it.responseData?.let { roomWithUsers ->
                            startChatScreenActivity(
                                requireActivity(),
                                roomWithUsers
                            )
                        }
                    }
                }

                else -> Timber.d("Room fetching error")
            }
        })

        viewModel.searchedMessageListener.observe(
            viewLifecycleOwner,
            EventObserver { searchMessage ->
                when (searchMessage.first.status) {
                    Resource.Status.SUCCESS -> {
                        // Sort searched messages by roomId and then by createdAt
                        val sortedList = searchMessage.first.responseData?.sortedWith(
                            compareBy(
                                { it.roomWithUsers?.room?.roomId },
                                { it.message.createdAt }
                            )
                        )
                        searchAdapter.setSearchedMessagesBold(searchMessage.second)
                        searchAdapter.submitList(null)
                        searchAdapter.submitList(sortedList)
                    }

                    else -> Timber.d("Search error")
                }
            })
    }

    private fun initializeViews() = with(binding) {
        btnSearchRooms.setOnClickListener {
            setUpButtons(searchRooms = true)
        }

        btnSearchMessages.setOnClickListener {
            setUpButtons(searchRooms = false)
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            menuItem.setOnActionExpandListener(object : OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    // Handle views when search view is opened
                    if (R.id.search_menu_icon == item.itemId) {
                        llSearchRoomsMessages.visibility = View.VISIBLE
                        fabNewRoom.visibility = View.GONE
                    }
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    // Handle views when search view is collapsed
                    if (R.id.search_menu_icon == item.itemId) {
                        llSearchRoomsMessages.visibility = View.GONE
                        rvMessages.visibility = View.GONE
                        rvRooms.visibility = View.VISIBLE
                        fabNewRoom.visibility = View.VISIBLE
                        btnSearchRooms.isSelected = true
                        btnSearchMessages.isSelected = false

                        searchAdapter.submitList(ArrayList())
                    }
                    return true
                }
            })

            when (menuItem.itemId) {
                R.id.search_menu_icon -> {
                    searchView = menuItem.actionView as SearchView
                    searchView?.let {
                        Tools.setUpSearchBar(
                            requireContext(),
                            it,
                            getString(R.string.contact_message_search)
                        )
                    }

                    setupSearchAdapter()
                    setSearch(searchView)

                    btnSearchRooms.backgroundTintList = primaryColor
                    btnSearchMessages.backgroundTintList = fourthAdditionalColor

                    true
                }

                else -> false
            }
        }

        fabNewRoom.setOnClickListener {
            findNavController().navigate(
                MainFragmentDirections.actionMainFragmentToNewRoomFragment(),
                navOptionsBuilder
            )
        }

        viewModel.roomUsers.clear()
    }

    private fun setUpButtons(
        searchRooms: Boolean
    ) = with(binding) {
        rvMessages.visibility = if (searchRooms) View.GONE else View.VISIBLE
        rvRooms.visibility = if (searchRooms) View.VISIBLE else View.GONE

        btnSearchMessages.backgroundTintList =
            if (searchRooms) fourthAdditionalColor else primaryColor
        btnSearchRooms.backgroundTintList = if (searchRooms) primaryColor else fourthAdditionalColor

        if (searchView != null) {
            searchView?.setQuery(searchQuery, true)
            setSearch(searchView)
        }
    }

    private fun setupAdapter() {
        roomsAdapter = RoomsAdapter(requireContext(), viewModel.getLocalUserId().toString()) {
            // TODO fetch room data for selected room and then navigate to it
            viewModel.getRoomWithUsers(it.roomWithUsers.room.roomId)
        }

        binding.rvRooms.apply {
            itemAnimator = null
            isMotionEventSplittingEnabled = false
            adapter = roomsAdapter
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        }
    }

    private fun setupSearchAdapter() {
        searchAdapter = SearchAdapter(viewModel.getLocalUserId().toString()) {
            searchMessageId = it.message.id
            if (searchMessageId != 0) {
                it.roomWithUsers?.room?.roomId?.let { roomId ->
                    viewModel.getRoomWithUsers(
                        roomId
                    )
                }
            }
        }

        binding.rvMessages.itemAnimator = null
        binding.rvMessages.adapter = searchAdapter
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvMessages.layoutManager = layoutManager
    }

    private fun setSearch(svRoomsSearch: SearchView?) {
        svRoomsSearch?.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    searchQuery = query
                    if (query.isNotEmpty()) {
                        if (binding.rvRooms.isVisible) {
                            userSearching = true
                            Timber.d("Query: $query")

                            // If room list is not empty, code will go through each element of the list
                            // and check if its name corresponds to the users query. Logic also handles
                            // private rooms with special logic, going through list of users in that
                            // room and selecting the one who's id is not the local user id.
                            if (sortedList.isNotEmpty()) {
                                val myUserId = viewModel.getLocalUserId().toString()
                                for (room in sortedList) {
                                    val shouldAddRoom =
                                        if (Const.JsonFields.PRIVATE == room.roomWithUsers.room.type) {
                                            room.roomWithUsers.users.any {
                                                myUserId != it.id.toString() && it.formattedDisplayName.lowercase()
                                                    .contains(query, ignoreCase = true)
                                            }
                                        } else {
                                            room.roomWithUsers.room.name?.lowercase()
                                                ?.contains(query, ignoreCase = true) == true
                                        }
                                    if (shouldAddRoom) {
                                        filteredList.add(room)
                                    }
                                }
                            }
                        } else {
                            viewModel.getSearchedMessages(query)
                        }
                    } else {
                        userSearching = false
                        roomsAdapter.submitList(sortedList)
                        searchAdapter.submitList(ArrayList())
                    }
                    roomsAdapter.submitList(ArrayList(filteredList))
                    filteredList.clear()
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    searchQuery = query
                    if (query.isNotEmpty()) {
                        if (binding.rvRooms.isVisible) {
                            userSearching = true
                            Timber.d("Query: $query")

                            // If room list is not empty, code will go through each element of the list
                            // and check if its name corresponds to the users query. Logic also handles
                            // private rooms with special logic, going through list of users in that
                            // room and selecting the one who's id is not the local user id.
                            if (sortedList.isNotEmpty()) {
                                val myUserId = viewModel.getLocalUserId().toString()
                                for (room in sortedList) {
                                    val shouldAddRoom =
                                        if (Const.JsonFields.PRIVATE == room.roomWithUsers.room.type) {
                                            room.roomWithUsers.users.any {
                                                myUserId != it.id.toString() && it.formattedDisplayName.lowercase()
                                                    .contains(query, ignoreCase = true)
                                            }
                                        } else {
                                            room.roomWithUsers.room.name?.lowercase()
                                                ?.contains(query, ignoreCase = true) == true
                                        }
                                    if (shouldAddRoom) {
                                        filteredList.add(room)
                                    }
                                }
                            }
                            roomsAdapter.submitList(ArrayList(filteredList))
                            filteredList.clear()
                        } else {
                            viewModel.getSearchedMessages(query)
                        }
                    } else {
                        userSearching = false
                        roomsAdapter.submitList(sortedList)
                        searchAdapter.submitList(ArrayList())
                    }
                }
                binding.rvRooms.scrollToPosition(0)
                return true
            }
        })
        svRoomsSearch?.setOnFocusChangeListener { view, hasFocus ->
            run {
                view.clearFocus()
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.topAppBar.menu.findItem(R.id.search_menu_icon).collapseActionView()
        searchQuery = ""
    }
}
