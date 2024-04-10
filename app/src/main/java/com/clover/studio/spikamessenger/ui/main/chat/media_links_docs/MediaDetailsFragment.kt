package com.clover.studio.spikamessenger.ui.main.chat.media_links_docs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentMediaDetailsBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.ui.main.chat.MediaScreenState
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import kotlinx.coroutines.launch
import timber.log.Timber

class MediaDetailsFragment(private val roomsWithUsers: RoomWithUsers?) : BaseFragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var binding: FragmentMediaDetailsBinding

    private var mediaList: List<Message> = arrayListOf()
    private var mediaAdapter: MediaAdapter? = null
    private var linearLayoutManager: LinearLayoutManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaDetailsBinding.inflate(layoutInflater)

        roomsWithUsers?.room?.roomId?.let { viewModel.getAllMediaWithOffset(roomId = it) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpAdapter()
        initializeListeners()
        getAllPhotos()
        Timber.d("Media details: $roomsWithUsers")
    }

    private fun setUpAdapter() {
        linearLayoutManager = GridLayoutManager(activity, 3)
        mediaAdapter = MediaAdapter(requireContext()) {
            // On click open image
        }

        binding.rvMediaItems.apply {
            itemAnimator = null
            isMotionEventSplittingEnabled = false
            adapter = mediaAdapter
            layoutManager = linearLayoutManager
        }
    }

    private fun initializeListeners() {
        binding.rvMediaItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lastVisiblePosition = linearLayoutManager?.findLastVisibleItemPosition()
                val totalItemCount = linearLayoutManager?.itemCount
                Timber.d("lastVisiblePosition: $lastVisiblePosition")
                Timber.d("totalItemCount: $totalItemCount")

                if (lastVisiblePosition != null && totalItemCount != null) {
                    if (lastVisiblePosition == totalItemCount.minus(1)) {
                        Timber.d("Fetching next batch of data")
                        roomsWithUsers?.room?.roomId?.let { viewModel.fetchNextMediaSet(roomId = it) }
                    }
                }
            }
        })
    }

    private fun getAllPhotos() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mediaState.collect { state ->
                    when (state) {
                        is MediaScreenState.Success -> {
                            if (state.media != null) {
                                mediaList = state.media
                                mediaAdapter?.submitList(mediaList)
                            }
                        }

                        is MediaScreenState.Error -> {
                            // Ignore
                        }

                        is MediaScreenState.Loading -> {
                            // ignore
                        }

                    }
                }
            }
        }
    }
}