package com.clover.studio.spikamessenger.ui.main.chat.media_links_docs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FragmentLinksBinding
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import timber.log.Timber

class LinksFragment(private val roomsWithUsers: RoomWithUsers?) : BaseFragment() {

    private lateinit var binding: FragmentLinksBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLinksBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("Links details: $roomsWithUsers")
    }
}