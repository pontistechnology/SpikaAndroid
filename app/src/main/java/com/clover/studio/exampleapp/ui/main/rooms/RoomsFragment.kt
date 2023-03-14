package com.clover.studio.exampleapp.ui.main.rooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.databinding.FragmentChatBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.ui.main.chat.startChatScreenActivity
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.google.gson.Gson
import timber.log.Timber

class RoomsFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var roomsAdapter: RoomsAdapter
    private var roomList: MutableList<RoomAndMessageAndRecords> = mutableListOf()
    private var filteredList: MutableList<RoomAndMessageAndRecords> = ArrayList()
    private var sortedList: MutableList<RoomAndMessageAndRecords> = ArrayList()
    private var bindingSetup: FragmentChatBinding? = null

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
                    Timber.d("Query: $query")
                    if (sortedList.isNotEmpty()) {
                        for (room in sortedList) {
                            if (room.roomWithUsers.room.name?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true
                            ) {
                                filteredList.add(room)
                            }
                        }
                    }
                    roomsAdapter.submitList(ArrayList(filteredList))
                    filteredList.clear()
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    Timber.d("Query: $query")
                    if (sortedList.isNotEmpty()) {
                        for (room in sortedList) {
                            if (room.roomWithUsers.room.name?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true
                            ) {
                                filteredList.add(room)
                            }
                        }
                    }
                    roomsAdapter.submitList(ArrayList(filteredList))
                    filteredList.clear()
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
        viewModel.getChatRoomAndMessageAndRecords().observe(viewLifecycleOwner) {
            if (it.responseData != null) {
                binding.tvNoChats.visibility = View.GONE

                roomList = it.responseData.toMutableList()
                val nonEmptyRoomList = it.responseData.filter { roomData ->
                    Const.JsonFields.GROUP == roomData.roomWithUsers.room.type || roomData.message?.isNotEmpty() == true
                }

                val pinnedRooms = roomList.filter { roomItem -> roomItem.roomWithUsers.room.pinned }
                    .sortedBy { pinnedRoom -> pinnedRoom.roomWithUsers.room.name }

                try {
                    sortedList =
                        nonEmptyRoomList.sortedWith(compareBy(nullsFirst()) { roomItem ->
                            if (!roomItem.message.isNullOrEmpty()) {
                                roomItem.message.last { message -> message.message.createdAt != null }.message.createdAt
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
                roomsAdapter.submitList(sortedList)
            }
        }
    }

    private fun setupAdapter() {
        roomsAdapter = RoomsAdapter(requireContext(), viewModel.getLocalUserId().toString()) {
            val gson = Gson()
            val roomData = gson.toJson(it.roomWithUsers)
            activity?.let { parent -> startChatScreenActivity(parent, roomData) }
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