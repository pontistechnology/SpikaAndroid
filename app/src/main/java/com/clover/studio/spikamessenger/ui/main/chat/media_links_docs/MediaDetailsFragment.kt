package com.clover.studio.spikamessenger.ui.main.chat.media_links_docs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentMediaDetailsBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.ui.main.chat.MediaScreenState
import com.clover.studio.spikamessenger.ui.main.chat.MediaType
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import kotlinx.coroutines.launch

class MediaDetailsFragment(private val roomsWithUsers: RoomWithUsers?) : BaseFragment() {
    private val viewModel: ChatViewModel by activityViewModels()
    private lateinit var binding: FragmentMediaDetailsBinding

    private var mediaAdapter: MediaAdapter? = null
    private var gridLayoutManager: GridLayoutManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaDetailsBinding.inflate(layoutInflater)

        roomsWithUsers?.room?.roomId?.let {
            viewModel.getAllMediaItemsWithOffset(
                roomId = it,
                mediaType = MediaType.MEDIA
            )
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpAdapter()
        initializeListeners()
        getAllPhotos()
    }

    private fun setUpAdapter() {
        gridLayoutManager = GridLayoutManager(activity, 3)

        mediaAdapter = MediaAdapter(
            context = requireContext(),
            mediaType = MediaType.MEDIA,
            roomWithUsers = roomsWithUsers
        ) { message, _ ->
            findNavController().navigate(
                MediaLinksDocsFragmentDirections.actionMediaLinksDocsFragmentToMediaFragment(
                    message = message,
                    roomWithUsers = roomsWithUsers
                )
            )
        }

        binding.rvMediaItems.apply {
            itemAnimator = null
            isMotionEventSplittingEnabled = false
            adapter = mediaAdapter
            layoutManager = gridLayoutManager
            addItemDecoration(
                StickyHeaderDecoration(
                    parent = this,
                    isHeader = { position -> mediaAdapter?.getItemViewType(position) == VIEW_TYPE_DATE_ITEM }
                )
            )
        }

        gridLayoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (mediaAdapter?.getItemViewType(position)) {
                    VIEW_TYPE_MEDIA_ITEM -> 1
                    VIEW_TYPE_DATE_ITEM -> 3
                    else -> 1
                }
            }
        }
    }

    private fun initializeListeners() {
        binding.rvMediaItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lastVisiblePosition = gridLayoutManager?.findLastVisibleItemPosition()
                val totalItemCount = gridLayoutManager?.itemCount
                if (lastVisiblePosition != null && totalItemCount != null) {
                    if (lastVisiblePosition == totalItemCount.minus(1)) {
                        roomsWithUsers?.room?.roomId?.let {
                            viewModel.fetchNextMediaSet(
                                roomId = it,
                                mediaType = MediaType.MEDIA
                            )
                        }
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
                            if (state.media?.isNotEmpty() == true) {
                                binding.tvNoMedia.visibility = View.GONE
                                val groupedMediaList = Tools.sortMediaItems(state.media)
                                mediaAdapter?.submitList(groupedMediaList)
                            } else {
                                binding.tvNoMedia.visibility = View.VISIBLE
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
