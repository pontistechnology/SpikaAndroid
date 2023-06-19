package com.clover.studio.exampleapp.ui.main.chat

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.databinding.FragmentMediaBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.helpers.MediaPlayer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
    private var picturePath: String = ""
    private var videoPath: String = ""

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
            picturePath = message?.body?.fileId?.let {
                Tools.getFilePathUrl(it)
            }.toString()
            initializePicture(picturePath)
        } else {
            videoPath = message?.body?.file?.id.let {
                Tools.getFilePathUrl(
                    it!!
                )
            }.toString()
            initializeVideo(videoPath)
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
            showBar()
        }

        binding.clMedia.setOnClickListener {
            showBar()
        }

        binding.vvVideo.setOnClickListener {
            showBar()
        }
    }

    private fun showBar() {
        val showBars =
            binding.clTopBar.visibility == View.GONE && binding.flBottomBar.visibility == View.GONE
        binding.clTopBar.animate().alpha(if (showBars) 1f else 0f).setDuration(BAR_ANIMATION)
            .withEndAction {
                binding.clTopBar.visibility = if (showBars) View.VISIBLE else View.GONE
            }.start()

        binding.flBottomBar.animate().alpha(if (showBars) 1f else 0f).setDuration(BAR_ANIMATION)
            .withEndAction {
                binding.flBottomBar.visibility = if (showBars) View.VISIBLE else View.GONE
            }.start()
    }

    private fun initializePicture(imagePath: String) {
        binding.clVideoLoading.visibility = View.GONE
        binding.clVideoContainer.visibility = View.GONE

        binding.clImageContainer.visibility = View.VISIBLE
        Glide.with(this)
            .load(imagePath)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.ivFullImage)
    }

    private fun initializeVideo(videoPath: String) {
        binding.clImageContainer.visibility = View.GONE
        binding.clVideoLoading.visibility = View.VISIBLE

        Glide.with(this)
            .load(videoPath)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
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

    // TODO download
    private fun downloadMedia() {
        val request = Request.Builder()
            .url("")
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val inputStream = response.body?.byteStream()
                val file = File(
                    context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "localId.${Const.FileExtensions.JPG}"
                )

                val outputStream = FileOutputStream(file)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                outputStream.close()
            }
        })
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
        if (message?.type == Const.JsonFields.VIDEO_TYPE) {
            initializeVideo(videoPath)
        }
    }

    override fun onResume() {
        super.onResume()
        if (message?.type == Const.JsonFields.VIDEO_TYPE) {
            initializeVideo(videoPath)
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
