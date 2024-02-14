package com.clover.studio.spikamessenger.ui.main.chat.media

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.databinding.ItemMediaBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.helpers.MediaPlayer
import timber.log.Timber

class MediaPagerAdapter(
    private val context: Context,
    private val mediaList: List<Message>,
    private val onItemClicked: (event: String, message: Message) -> Unit
) :
    RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder>() {

    private var player: ExoPlayer? = null
    private var playbackStateListener: Player.Listener = playbackStateListener()

    inner class MediaViewHolder(val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun getItemCount(): Int = mediaList.size

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        if (Const.JsonFields.IMAGE_TYPE == mediaList[position].type) {
            bindMediaImage(holder.binding, position)
            holder.binding.clVideoContainer.visibility = View.GONE
        } else {
            bindMediaVideo(holder.binding, position)
            holder.binding.clImageContainer.visibility = View.GONE
        }
    }

    override fun onViewAttachedToWindow(holder: MediaViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.absoluteAdapterPosition

        if (Const.JsonFields.VIDEO_TYPE == mediaList[position].type) {
            bindMediaVideo(holder.binding, position)
        }
    }

    private fun bindMediaVideo(binding: ItemMediaBinding, position: Int) {
        releasePlayer()

        player = MediaPlayer.getInstance(context)
            .also { exoPlayer ->
                binding.vvVideo.player = exoPlayer

                val videoPath = mediaList[position].body?.file?.id.let {
                    Tools.getFilePathUrl(
                        it ?: 0L
                    )
                }.toString()

                val mediaItem = MediaItem.fromUri(Uri.parse(videoPath))

                exoPlayer.apply {
                    setMediaItem(mediaItem)
                    playWhenReady = true
                    addListener(playbackStateListener())
                    prepare()
                }
            }

        binding.clImageContainer.visibility = View.GONE
        binding.clVideoContainer.visibility = View.VISIBLE

        binding.clVideoContainer.setOnClickListener { _ ->
            onItemClicked.invoke(Const.MediaActions.MEDIA_SHOW_BARS, mediaList[position])
        }
    }

    private fun bindMediaImage(binding: ItemMediaBinding, position: Int) {
        binding.clImageContainer.visibility = View.VISIBLE
        binding.ivFullImage.setOnClickListener { _ ->
            onItemClicked.invoke(Const.MediaActions.MEDIA_SHOW_BARS, mediaList[position])
        }

        val imagePath = mediaList[position].body?.fileId?.let {
            Tools.getFilePathUrl(it)
        }.toString()

        Glide.with(context)
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
                    binding.pbMediaImage.visibility = View.GONE
                    return false
                }
            })
            .into(binding.ivFullImage)
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED"
                else -> "UNKNOWN_STATE"
            }
            Timber.d("State:: $stateString")
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            exoPlayer.stop()
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()

            MediaPlayer.resetPlayer()
            Timber.d("Player released")
        }
    }
}

