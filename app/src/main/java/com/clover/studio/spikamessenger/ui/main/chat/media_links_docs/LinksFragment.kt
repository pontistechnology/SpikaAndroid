package com.clover.studio.spikamessenger.ui.main.chat.media_links_docs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentLinksBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.ui.main.chat.MediaScreenState
import com.clover.studio.spikamessenger.ui.main.chat.MediaType
import com.clover.studio.spikamessenger.utils.Tools.getMonthFromTimestamp
import com.clover.studio.spikamessenger.utils.Tools.makeDateMessage
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import kotlinx.coroutines.launch
import timber.log.Timber

class LinksFragment(private val roomsWithUsers: RoomWithUsers?) : BaseFragment() {

    private lateinit var binding: FragmentLinksBinding
    private val viewModel: ChatViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLinksBinding.inflate(layoutInflater)

        roomsWithUsers?.room?.roomId?.let { viewModel.getAllMediaItemsWithOffset(roomId = it, MediaType.LINKS) }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("Links details: $roomsWithUsers")
        getAllLinks()
    }

    private fun getAllLinks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.linksState.collect { state ->
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