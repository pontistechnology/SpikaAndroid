package com.clover.studio.spikamessenger.ui.main.chat.media_links_docs

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentMediaLinksDocsBinding
import com.clover.studio.spikamessenger.ui.main.MainPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import timber.log.Timber

class MediaLinksDocsFragment : Fragment() {
    private lateinit var binding: FragmentMediaLinksDocsBinding
    private var roomsWithUsers : RoomWithUsers? = null
    private val args: MediaLinksDocsFragmentArgs by navArgs()

    private val tabNames = arrayOf("Media", "Links", "Docs")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomsWithUsers = args.roomWithUsers

        Timber.d("Room with users: $roomsWithUsers")
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