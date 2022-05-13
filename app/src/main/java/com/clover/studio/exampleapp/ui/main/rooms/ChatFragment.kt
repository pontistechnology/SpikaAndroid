package com.clover.studio.exampleapp.ui.main.rooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.data.models.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.databinding.FragmentChatBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.ui.main.RoomFetchFail
import com.clover.studio.exampleapp.ui.main.RoomsFetched
import com.clover.studio.exampleapp.ui.main.chat.startChatScreenActivity
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.google.gson.Gson
import timber.log.Timber

class ChatFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var roomList: List<RoomAndMessageAndRecords>
    private var filteredList: MutableList<RoomAndMessageAndRecords> = ArrayList()

    private var bindingSetup: FragmentChatBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentChatBinding.inflate(inflater, container, false)

        setupAdapter()
        initializeObservers()
        setupSearchView()

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
                    if (::roomList.isInitialized) {
                        for (room in roomList) {
                            if (Const.JsonFields.PRIVATE == room.room.type) {
                                room.room.users?.forEach { roomUser ->
                                    if (viewModel.getLocalUserId()
                                            .toString() != roomUser.userId.toString()
                                    ) {
                                        if (roomUser.user?.displayName?.lowercase()
                                                ?.contains(query, ignoreCase = true) == true
                                        ) {
                                            filteredList.add(room)
                                        }
                                    }
                                }
                            } else {
                                if (room.room.name?.lowercase()
                                        ?.contains(query, ignoreCase = true) == true
                                ) {
                                    filteredList.add(room)
                                }
                            }
                        }
                        val sortedList =
                            filteredList.filter { roomItem -> !roomItem.message.isNullOrEmpty() }
                                .sortedByDescending { roomItem ->
                                    roomItem.message?.first { message -> message.message.createdAt != null }?.message?.createdAt
                                }
                        roomsAdapter.submitList(ArrayList(sortedList))
                        filteredList.clear()
                    }
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    Timber.d("Query: $query")
                    if (::roomList.isInitialized) {
                        for (room in roomList) {
                            if (Const.JsonFields.PRIVATE == room.room.type) {
                                room.room.users?.forEach { roomUser ->
                                    if (viewModel.getLocalUserId()
                                            .toString() != roomUser.userId.toString()
                                    ) {
                                        if (roomUser.user?.displayName?.lowercase()
                                                ?.contains(query, ignoreCase = true) == true
                                        ) {
                                            filteredList.add(room)
                                        }
                                    }
                                }
                            } else {
                                if (room.room.name?.lowercase()
                                        ?.contains(query, ignoreCase = true) == true
                                ) {
                                    filteredList.add(room)
                                }
                            }
                        }
                        Timber.d("Filtered List: $filteredList")
                        val sortedList =
                            filteredList.filter { roomItem -> !roomItem.message.isNullOrEmpty() }
                                .sortedByDescending { roomItem ->
                                    roomItem.message?.first { message -> message.message.createdAt != null }?.message?.createdAt
                                }
                        roomsAdapter.submitList(ArrayList(sortedList))
                        filteredList.clear()
                    }
                }
                return true
            }
        })
        binding.svRoomsSearch.setOnFocusChangeListener { view, hasFocus ->
            run {
                view.clearFocus()
                if (!hasFocus) {
                    Tools.hideKeyboard(requireActivity(), view)
                }
            }
        }
    }

    private fun initializeObservers() {
        viewModel.roomsListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                RoomFetchFail -> Timber.d("Failed to fetch rooms")
                RoomsFetched -> Timber.d("Rooms fetched successfully")
                else -> Timber.d("Other error")
            }
        })

//        viewModel.getRooms().observe(viewLifecycleOwner) {
//            if (it.isNotEmpty()) {
//                binding.tvNoChats.visibility = View.GONE
//                roomList = it
//                roomsAdapter.submitList(it)
//            }
//        }

        viewModel.getChatRoomAndMessageAndRecords().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.tvNoChats.visibility = View.GONE
                for (roomData in it) {
                    Timber.d("RoomData ${roomData.room.roomId}")
                }
                roomList = it
                val sortedList = it.filter { roomItem -> !roomItem.message.isNullOrEmpty() }
                    .sortedByDescending { roomItem ->
                        roomItem.message?.first { message -> message.message.createdAt != null }?.message?.createdAt
                    }
                roomsAdapter.submitList(sortedList)
            }
        }
    }

    private fun setupAdapter() {
        roomsAdapter = RoomsAdapter(requireContext(), viewModel.getLocalUserId().toString()) {
            val gson = Gson()
            val roomData = gson.toJson(it.room)
            activity?.let { parent -> startChatScreenActivity(parent, roomData) }
        }

        binding.rvRooms.adapter = roomsAdapter
        binding.rvRooms.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getRoomsRemote()
    }
}