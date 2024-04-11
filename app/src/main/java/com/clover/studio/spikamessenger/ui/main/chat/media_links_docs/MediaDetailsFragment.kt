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
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentMediaDetailsBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.ui.main.chat.MediaScreenState
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

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

        roomsWithUsers?.room?.roomId?.let { viewModel.getAllMediaWithOffset(roomId = it) }

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

        mediaAdapter = MediaAdapter(requireContext()) {
            findNavController().navigate(
                MediaLinksDocsFragmentDirections.actionMediaLinksDocsFragmentToMediaFragment(
                    message = it,
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
                                val groupedMediaList = mutableListOf<Message>()

                                state.media
                                    .sortedByDescending { it.createdAt }
                                    .groupBy { getMonthFromTimestamp(it.createdAt ?: 0) }
                                    .forEach { (month, mediaItems) ->
                                        groupedMediaList.add(makeDateMessage(month))
                                        groupedMediaList.addAll(mediaItems)
                                    }

                                mediaAdapter?.submitList(groupedMediaList)
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

    private fun makeDateMessage(month: String): Message {
        return Message(
            id = 0,
            fromUserId = null,
            totalUserCount = null,
            deliveredCount = null,
            seenCount = null,
            roomId = null,
            type = Const.JsonFields.TEXT_TYPE,
            body = MessageBody(
                referenceMessage = null,
                text = month,
                thumbId = null,
                fileId = null,
                file = null,
                thumb = null,
                thumbnailData = null,
                type = Const.JsonFields.TEXT_TYPE,
                subject = null,
                subjectId = null,
                objectIds = null,
                objects = null
            ),
            referenceMessage = null,
            createdAt = null,
            modifiedAt = null,
            deleted = null,
            replyId = null,
            localId = null,
            messageStatus = null,
            uri = null,
            thumbUri = null,
            unreadCount = 0,
            userName = "",
            isForwarded = false
        )
    }

    private fun getMonthFromTimestamp(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        val year = calendar.get(Calendar.YEAR)
        return "$month $year"
    }

    // For testing
//    private fun getMonthAndDayFromTimestamp(timestamp: Long): String {
//        val calendar = Calendar.getInstance()
//        calendar.timeInMillis = timestamp
//        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
//        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
//        val year = calendar.get(Calendar.YEAR)
//        return "$month $dayOfMonth, $year"
//    }
}