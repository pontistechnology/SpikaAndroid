package com.clover.studio.spikamessenger.ui.main.chat.media_links_docs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentMediaLinksDocsBinding
import com.clover.studio.spikamessenger.ui.main.MainPagerAdapter
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.google.android.material.tabs.TabLayoutMediator

class MediaLinksDocsFragment : Fragment() {
    private lateinit var binding: FragmentMediaLinksDocsBinding
    private var roomsWithUsers: RoomWithUsers? = null

    private val tabNames: MutableList<String> = mutableListOf()
    private val viewModel: ChatViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomsWithUsers = arguments?.getParcelable(Const.Navigation.ROOM_DATA)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaLinksDocsBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabNames.addAll(
            listOf(
                getString(R.string.media_items),
                getString(R.string.link_items),
                getString(R.string.doc_items)
            )
        )
        initializePager()
        initializeViews()
    }

    private fun initializeViews() {
        binding.tvTitle.text = if (roomsWithUsers?.room?.type == Const.JsonFields.PRIVATE) {
            roomsWithUsers?.room?.name
        } else {
            roomsWithUsers?.users?.find { it.id != viewModel.getLocalUserId() }?.displayName.toString()
        }

        binding.ivBackArrow.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initializePager() {
        val fragmentList = arrayListOf(
            MediaDetailsFragment(roomsWithUsers = roomsWithUsers),
            LinksFragment(roomsWithUsers = roomsWithUsers),
            DocsFragment(roomsWithUsers = roomsWithUsers)
        )

        val pagerAdapter =
            MainPagerAdapter(
                fragmentList,
                this.childFragmentManager,
                lifecycle
            )

        binding.viewPager.adapter = pagerAdapter

        initializeTab()
    }

    private fun initializeTab() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabNames[position]
        }.attach()
    }
}