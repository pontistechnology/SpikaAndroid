package com.clover.studio.spikamessenger.ui.main.chat

import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.databinding.FragmentMediaBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.dialog.ChooserDialog
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.helpers.MediaPlayer

const val BAR_ANIMATION = 500L

class MediaFragment : BaseFragment() {

    private var bindingSetup: FragmentMediaBinding? = null
    private val binding get() = bindingSetup!!
    private val args: MediaFragmentArgs by navArgs()

    private var player: ExoPlayer? = null

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L
    private val playbackStateListener: Player.Listener = playbackStateListener()

    private var mediaInfo: String? = null
    private var message: Message? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaInfo = args.mediaInfo
        message = args.message
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentMediaBinding.inflate(inflater, container, false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        initializeViews()
        initializeListeners()

        if (message?.type == Const.JsonFields.IMAGE_TYPE) {
            initializePicture()
        } else {
            initializeVideo()
        }

        return binding.root
    }

    private fun initializeViews() {
        binding.tvMediaInfo.text = mediaInfo
    }

    private fun initializeListeners() = with(binding) {
        ivBackToChat.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        ivFullImage.setOnClickListener {
            showBar()
        }

        clMedia.setOnClickListener {
            showBar()
        }

        vvVideo.setOnClickListener {
            showBar()
        }

        tvMoreMedia.setOnClickListener {
            ChooserDialog.getInstance(
                requireContext(),
                null,
                null,
                getString(R.string.download_media),
                null,
                object : DialogInteraction {
                    override fun onFirstOptionClicked() {
                        message?.let { msg ->
                            Tools.downloadFile(
                                requireContext(),
                                msg,
                            )
                        }
                    }

                    override fun onSecondOptionClicked() {
                        // Ignore
                    }
                }
            )
        }
    }

    private fun showBar() = with(binding) {
        val showBars =
            clTopBar.visibility == View.GONE && flBottomBar.visibility == View.GONE
        clTopBar.animate().alpha(if (showBars) 1f else 0f).setDuration(BAR_ANIMATION)
            .withEndAction {
                clTopBar.visibility = if (showBars) View.VISIBLE else View.GONE
            }.start()

        flBottomBar.animate().alpha(if (showBars) 1f else 0f).setDuration(BAR_ANIMATION)
            .withEndAction {
                flBottomBar.visibility = if (showBars) View.VISIBLE else View.GONE
            }.start()
    }

    private fun initializePicture() = with(binding) {
        val imagePath = message?.body?.fileId?.let {
            Tools.getFilePathUrl(it)
        }.toString()

        clVideoContainer.visibility = View.GONE
        clImageContainer.visibility = View.VISIBLE

        Glide.with(this@MediaFragment)
            .load(imagePath)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    // Ignore
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    pbMediaImage.visibility = View.GONE
                    return false
                }
            })
            .into(ivFullImage)

    }

    private fun initializeVideo() = with(binding) {
        val videoPath = message?.body?.file?.id.let {
            Tools.getFilePathUrl(
                it!!
            )
        }.toString()

        clImageContainer.visibility = View.GONE

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
                }
        }
        clVideoContainer.visibility = View.VISIBLE
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
        if (Const.JsonFields.VIDEO_TYPE == message?.type) {
            initializeVideo()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Const.JsonFields.VIDEO_TYPE == message?.type) {
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
