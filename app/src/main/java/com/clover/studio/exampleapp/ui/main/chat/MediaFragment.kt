package com.clover.studio.exampleapp.ui.main.chat

import android.content.pm.ActivityInfo
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
import com.clover.studio.exampleapp.databinding.FragmentMediaBinding


class MediaFragment : Fragment() {

    private var bindingSetup: FragmentMediaBinding? = null
    private val binding get() = bindingSetup!!
    private val args: MediaFragmentArgs by navArgs()

    private var clicked = true
    private var mediaController: MediaController? = null

    private var videoPath: String? = null
    private var imagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoPath = args.videoPath
        imagePath = args.picturePath
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentMediaBinding.inflate(inflater, container, false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        initializeListeners()
        if (imagePath!!.isEmpty()) {
            initializeVideo()
        } else {
            initializePicture()

        }

        return binding.root
    }

    private fun initializeListeners() {
        binding.ivBackToChat.setOnClickListener {
            val action = MediaFragmentDirections.actionVideoFragmentToChatMessagesFragment()
            findNavController().navigate(action)
        }

        binding.clImageContainer.setOnClickListener {
            showBackArrow()
        }

        binding.clVideoContainer.setOnClickListener {
            showBackArrow()
        }
    }

    private fun showBackArrow() {
        if (clicked) {
            binding.clBackArrow.visibility = View.VISIBLE
        } else {
            binding.clBackArrow.visibility = View.GONE
        }
        clicked = !clicked
    }

    private fun initializePicture() {
        binding.clVideoLoading.visibility = View.GONE
        binding.clVideoContainer.visibility = View.GONE

        binding.clImageContainer.visibility = View.VISIBLE
        Glide.with(this)
            .load(imagePath)
            .into(binding.ivFullImage)
    }

    private fun initializeVideo() {
        binding.clImageContainer.visibility = View.GONE
        binding.clVideoLoading.visibility = View.VISIBLE

        Glide.with(this)
            .load(videoPath)
            .into(binding.ivVideoHolder)

        val videoView = binding.vvVideo
        mediaController = MediaController(context)
        mediaController!!.setAnchorView(binding.clVideoContainer)

        videoView.setMediaController(mediaController)
        videoView.setVideoURI(Uri.parse(videoPath))
        videoView.setOnPreparedListener {
            binding.clVideoLoading.visibility = View.GONE
        }
        binding.clVideoContainer.visibility = View.VISIBLE
        videoView.start()
    }
}