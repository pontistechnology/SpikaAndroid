package com.clover.studio.spikamessenger.ui.main.chat.media_links_docs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentMediaLinksDocsBinding
import com.clover.studio.spikamessenger.ui.main.MainPagerAdapter
import com.clover.studio.spikamessenger.utils.Const
import com.google.android.material.tabs.TabLayoutMediator

class MediaLinksDocsFragment : Fragment() {
    private lateinit var binding: FragmentMediaLinksDocsBinding
    private var roomsWithUsers: RoomWithUsers? = null

    private val tabNames = arrayOf("Media", "Links", "Docs")

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

        initializePager()
        initializeViews()
    }

    private fun initializeViews() {
        binding.tvTitle.text = roomsWithUsers?.room?.name
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