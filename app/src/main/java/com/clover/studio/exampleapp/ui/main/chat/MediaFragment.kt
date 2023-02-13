package com.clover.studio.exampleapp.ui.main.chat

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.databinding.FragmentMediaBinding
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.helpers.MediaPlayer


class MediaFragment : BaseFragment() {

    private var bindingSetup: FragmentMediaBinding? = null
    private val binding get() = bindingSetup!!
    private val args: MediaFragmentArgs by navArgs()

    private var clicked = false
    private var player: ExoPlayer? = null

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L
    private val playbackStateListener: Player.Listener = playbackStateListener()

    private var videoPath: String? = null
    private var imagePath: String? = null
    private var mediaInfo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        videoPath = args.videoPath
        imagePath = args.picturePath
        mediaInfo = args.mediaInfo
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentMediaBinding.inflate(inflater, container, false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        initializeViews()
        initializeListeners()
        if (imagePath?.isEmpty() == true) {
            initializeVideo()
        } else {
            initializePicture()
        }

        return binding.root
    }

    private fun initializeViews() {
        binding.tvMediaInfo.text = mediaInfo
    }

    private fun initializeListeners() {
        binding.ivBackToChat.setOnClickListener {
            val action = MediaFragmentDirections.actionVideoFragmentToChatMessagesFragment()
            findNavController().navigate(action)
        }

        // This is listener for zoom on image and for showing/removing top layout
        binding.ivFullImage.setOnClickListener {
            showBackArrow()
        }

        binding.clMedia.setOnClickListener {
            showBackArrow()
        }
    }

    private fun showBackArrow() {
        if (clicked) {
            binding.clBackArrow.alpha = 0f
            binding.clBackArrow.visibility = View.VISIBLE
            binding.clBackArrow.animate().alpha(1f).setDuration(500).start()
        } else {
            binding.clBackArrow.animate().alpha(0f).setDuration(500).withEndAction {
                binding.clBackArrow.visibility = View.GONE
            }.start()
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

        player = context?.let {
            MediaPlayer.getInstance(it)
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
            exoPlayer.stop()
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()

            // Since ExoPlayer is released, we need to reset the singleton instance to null. Instead
            // we won't be able to use ExoPlayer instance anymore since it is released.
            MediaPlayer.resetPlayer()
        }
    }

    override fun onStart() {
        super.onStart()
        if (imagePath?.isEmpty() == true) {
            initializeVideo()
        }
    }

    override fun onResume() {
        super.onResume()
        if (imagePath?.isEmpty() == true) {
            initializeVideo()
        }
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
