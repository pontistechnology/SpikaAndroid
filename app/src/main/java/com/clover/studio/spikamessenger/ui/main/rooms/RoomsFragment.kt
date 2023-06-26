package com.clover.studio.spikamessenger.ui.main.rooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.RoomWithLatestMessage
import com.clover.studio.spikamessenger.databinding.FragmentChatBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.chat.startChatScreenActivity
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import timber.log.Timber

class RoomsFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var roomsAdapter: RoomsAdapter
    private var roomList: MutableList<RoomWithLatestMessage> = mutableListOf()
    private var filteredList: MutableList<RoomWithLatestMessage> = ArrayList()
    private var sortedList: MutableList<RoomWithLatestMessage> = ArrayList()
    private var bindingSetup: FragmentChatBinding? = null

    private var userSearching = false

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentChatBinding.inflate(inflater, container, false)

        initializeObservers()
        setupAdapter()
        setupSearchView()

        binding.ivCreateRoom.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_newRoomFragment)
        }

        return binding.root
    }

    private fun setupSearchView() {
        // SearchView is immediately acting as if selected
        binding.svRoomsSearch.setIconifiedByDefault(false)
        binding.svRoomsSearch.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    if (query.isNotEmpty()) {
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
                        userSearching = false
                        roomsAdapter.submitList(sortedList)
                    }
                    roomsAdapter.submitList(ArrayList(filteredList))
                    filteredList.clear()
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    if (query.isNotEmpty()) {
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
                        userSearching = false
                        roomsAdapter.submitList(sortedList)
                    }
                }
                binding.rvRooms.scrollToPosition(0)
                return true
            }
        })
        binding.svRoomsSearch.setOnFocusChangeListener { view, hasFocus ->
            run {
                view.clearFocus()
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
        }
    }

    private fun initializeObservers() {
        viewModel.getChatRoomsWithLatestMessage().observe(viewLifecycleOwner) {
            if (it.responseData != null) {
                binding.tvNoChats.visibility = View.GONE

                roomList = it.responseData.toMutableList()

                val nonEmptyRoomList = it.responseData.filter { roomData ->
                    Const.JsonFields.GROUP == roomData.roomWithUsers.room.type || roomData.message != null
                }

                val pinnedRooms = roomList.filter { roomItem -> roomItem.roomWithUsers.room.pinned }
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
            }
        }
    }

    private fun setupAdapter() {
        roomsAdapter = RoomsAdapter(requireContext(), viewModel.getLocalUserId().toString()) {
            activity?.let { parent -> startChatScreenActivity(parent, it.roomWithUsers) }
        }

        binding.rvRooms.itemAnimator = null
        binding.rvRooms.adapter = roomsAdapter
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvRooms.layoutManager = layoutManager
    }

    override fun onResume() {
        super.onResume()
//        viewModel.getRooms()

        // This updates the elapsed time displayed when user return to the screen.
        // TODO: it needs to be removed at one point
        roomsAdapter.notifyDataSetChanged()
    }
}