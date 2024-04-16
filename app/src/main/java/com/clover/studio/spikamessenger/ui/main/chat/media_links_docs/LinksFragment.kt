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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentLinksBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.ui.main.chat.MediaScreenState
import com.clover.studio.spikamessenger.ui.main.chat.MediaType
import com.clover.studio.spikamessenger.utils.Tools.sortMediaItems
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import kotlinx.coroutines.launch

class LinksFragment(private val roomsWithUsers: RoomWithUsers?) : BaseFragment() {

    private lateinit var binding: FragmentLinksBinding
    private val viewModel: ChatViewModel by activityViewModels()
    private var mediaAdapter: MediaAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLinksBinding.inflate(layoutInflater)

        roomsWithUsers?.room?.roomId?.let {
            viewModel.getAllMediaItemsWithOffset(
                roomId = it,
                MediaType.LINKS
            )
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getAllLinks()
        setUpAdapter()
    }

    private fun getAllLinks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.linksState.collect { state ->
                    when (state) {
                        is MediaScreenState.Success -> {
                            if (state.media != null) {
                                val groupedMediaList = sortMediaItems(state.media)
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

    private fun setUpAdapter() {
        mediaAdapter = MediaAdapter(
            requireContext(),
            roomWithUsers = roomsWithUsers,
            mediaType = MediaType.LINKS
        ) { message, _ ->
            viewModel.searchMessageId.value = message.id
            findNavController().navigate(MediaLinksDocsFragmentDirections.actionMediaLinksDocsFragmentToChatMessagesFragment())
        }

        binding.rvLinks.apply {
            itemAnimator = null
            isMotionEventSplittingEnabled = false
            adapter = mediaAdapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            addItemDecoration(
                StickyHeaderDecoration(
                    parent = this,
                    isHeader = { position -> mediaAdapter?.getItemViewType(position) == VIEW_TYPE_DATE_ITEM }
                )
            )
        }
    }
}