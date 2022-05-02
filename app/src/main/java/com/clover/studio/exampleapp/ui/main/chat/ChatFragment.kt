package com.clover.studio.exampleapp.ui.main.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.clover.studio.exampleapp.databinding.FragmentChatBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.ui.main.RoomFetchFail
import com.clover.studio.exampleapp.ui.main.RoomsFetched
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import timber.log.Timber

class ChatFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()

    private var bindingSetup: FragmentChatBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentChatBinding.inflate(inflater, container, false)

        initializeObservers()
        setupAdapter()

        return binding.root
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
                // TODO submit room list to adapter
            }
        }
    }

    private fun setupAdapter() {
        TODO("Not yet implemented")
    }
}