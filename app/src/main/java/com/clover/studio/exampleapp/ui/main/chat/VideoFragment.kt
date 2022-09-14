package com.clover.studio.exampleapp.ui.main.chat

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.databinding.FragmentVideoBinding


class VideoFragment : Fragment() {

    private var bindingSetup: FragmentVideoBinding? = null
    private val binding get() = bindingSetup!!
    private val args: VideoFragmentArgs by navArgs()
    private var videoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoPath = args.videoPath
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentVideoBinding.inflate(inflater, container, false)

        initializeListeners()
        initializeVideo()

        return binding.root
    }

    private fun initializeListeners() {
        binding.ivBackToChat.setOnClickListener {
            val action = VideoFragmentDirections.actionVideoFragmentToChatMessagesFragment()
            findNavController().navigate(action)
        }
    }

    private fun initializeVideo() {
        Glide.with(this)
            .load(videoPath)
            .into(binding.ivVideoHolder)

        val videoView = binding.vvVideo
        val mediaController = MediaController(context)
        mediaController.setAnchorView(binding.clVideoContainer)

        videoView.setMediaController(mediaController)
        videoView.setVideoURI(Uri.parse(videoPath))
        videoView.setOnPreparedListener {
            binding.clVideoLoading.visibility = View.GONE
        }

        binding.clVideoContainer.visibility = View.VISIBLE
        videoView.start()
    }
}