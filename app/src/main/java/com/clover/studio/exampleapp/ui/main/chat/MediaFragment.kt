package com.clover.studio.exampleapp.ui.main.chat

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.databinding.FragmentMediaBinding


class MediaFragment : Fragment() {

    private var bindingSetup: FragmentMediaBinding? = null
    private val binding get() = bindingSetup!!
    private val args: MediaFragmentArgs by navArgs()

    private var clicked = true
    private var player: ExoPlayer? = null

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L
    private val playbackStateListener: Player.Listener = playbackStateListener()

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

        val trackSelector = DefaultTrackSelector(requireContext()).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        player = context?.let {
            ExoPlayer.Builder(it)
                .setTrackSelector(trackSelector)
                .build()
                .also { exoPlayer ->
                    binding.vvVideo.player = exoPlayer

                    // TODO adaptive streaming, look at this later
//                    val mediaItem = MediaItem.Builder()
//                        .setUri(Uri.parse(videoPath))
//                        .setMimeType(MimeTypes.APPLICATION_MPD)
//                        .build()

                    val mediaItem = MediaItem.fromUri(Uri.parse(videoPath))
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.playWhenReady = playWhenReady
                    exoPlayer.seekTo(currentItem, playbackPosition)
                    exoPlayer.addListener(playbackStateListener)
                    exoPlayer.prepare()
                    binding.clVideoLoading.visibility = View.GONE
                }
        }
        binding.clVideoContainer.visibility = View.VISIBLE
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()
        }
    }

    override fun onStart() {
        super.onStart()
        initializeVideo()
    }

    override fun onResume() {
        super.onResume()
        initializeVideo()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
}

private fun playbackStateListener() = object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        val stateString: String = when (playbackState) {
            ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
            ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
            ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
            ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
            else -> "UNKNOWN_STATE             -"
        }
    }
}
