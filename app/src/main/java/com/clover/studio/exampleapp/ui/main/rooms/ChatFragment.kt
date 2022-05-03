package com.clover.studio.exampleapp.ui.main.rooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.databinding.FragmentChatBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.ui.main.RoomFetchFail
import com.clover.studio.exampleapp.ui.main.RoomsFetched
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import timber.log.Timber

class ChatFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var roomList: List<ChatRoom>
    private var filteredList: MutableList<ChatRoom> = ArrayList()

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
                            if (room.name?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true
                            ) {
                                filteredList.add(room)
                            }
                        }
                        roomsAdapter.submitList(ArrayList(filteredList))
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
                            if (room.name?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true
                            ) {
                                filteredList.add(room)
                            }
                        }
                        Timber.d("Filtered List: $filteredList")
                        roomsAdapter.submitList(ArrayList(filteredList))
                        filteredList.clear()
                    }
                }
                return true
            }
        })
    }

    private fun initializeObservers() {
        viewModel.roomsListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                RoomFetchFail -> Timber.d("Failed to fetch rooms")
                RoomsFetched -> Timber.d("Rooms fetched successfully")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getRooms().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.tvNoChats.visibility = View.GONE
                roomList = it
                roomsAdapter.submitList(it)
            }
        }
    }

    private fun setupAdapter() {
        roomsAdapter = RoomsAdapter(requireContext(), viewModel.getLocalUserId().toString()) {
//            val bundle = bundleOf(Const.Navigation.ROOM_DATA to it)
//            startChatScreenActivity(activity, )
            // TODO navigate to room
//            findNavController().navigate(R.id.action_mainFragment_to_contactDetailsFragment, bundle)
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