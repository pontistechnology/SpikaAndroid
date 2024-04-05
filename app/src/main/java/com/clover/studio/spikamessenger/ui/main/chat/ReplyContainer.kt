package com.clover.studio.spikamessenger.ui.main.chat

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.ReplyActionBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools

class ReplyContainer(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    private var bindingSetup: ReplyActionBinding = ReplyActionBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup

    private var listener: ReplyContainerListener? = null

    interface ReplyContainerListener {
        fun closeSheet()
    }

    fun setReplyContainerListener(listener: ReplyContainerListener) {
        this.listener = listener
    }

    init {
        binding.ivRemove.setOnClickListener {
            binding.clMessageReply.visibility = View.GONE
            listener?.closeSheet()
        }
        binding.clMessageReply.visibility = View.GONE
    }

    fun closeBottomSheet() {
        binding.clMessageReply.visibility = View.GONE
    }

    fun isReplyBottomSheetVisible(): Boolean {
        return binding.clMessageReply.visibility == View.VISIBLE
    }

    fun setReactionContainer(
        message: Message,
        roomWithUsers: RoomWithUsers,
    ) = with(binding) {
        clMessageReply.visibility = View.VISIBLE

        tvUsername.apply {
            text = roomWithUsers.users.firstOrNull {
                it.id == message.fromUserId
            }?.formattedDisplayName
            visibility = if (roomWithUsers.room.type == Const.JsonFields.PRIVATE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        val mediaPath = Tools.getMediaPath(context, message)
        when (message.type) {
            Const.JsonFields.IMAGE_TYPE -> {
                if (message.body?.file?.mimeType?.contains(Const.FileExtensions.GIF) == true) {
                    setupMediaType(
                        R.string.gif,
                        R.drawable.img_gif_small,
                        mediaPath
                    )
                } else {
                    setupMediaType(
                        R.string.photo,
                        R.drawable.img_camera,
                        mediaPath
                    )
                }
            }

            Const.JsonFields.VIDEO_TYPE -> setupMediaType(
                R.string.video,
                R.drawable.img_video_reply,
                mediaPath
            )

            Const.JsonFields.AUDIO_TYPE -> setupMediaType(
                R.string.audio,
                R.drawable.img_audio_reply,
                null
            )

            Const.JsonFields.FILE_TYPE -> setupMediaType(
                R.string.file,
                R.drawable.img_file_reply,
                null
            )

            else -> {
                if (message.body?.thumbnailData != null) {
                    val image = message.body.thumbnailData?.image

                    Glide.with(context)
                        .load(image)
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .placeholder(R.drawable.img_image_placeholder)
                        .dontTransform()
                        .error(R.drawable.img_image_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivReplyImage)
                    ivReplyImage.visibility = View.VISIBLE
                } else ivReplyImage.visibility = View.GONE

                tvReplyMedia.visibility = View.VISIBLE
                tvReplyMedia.text = message.body?.text
                tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
    }

    private fun setupMediaType(textResId: Int, drawableResId: Int, mediaPath: String?) =
        with(binding) {
            ivReplyImage.visibility = View.VISIBLE

            tvReplyMedia.apply {
                text = context.getString(R.string.media, context.getString(textResId))
                setCompoundDrawablesWithIntrinsicBounds(drawableResId, 0, 0, 0)
            }

            if (mediaPath != null) {
                tvReplyMedia.visibility = View.VISIBLE
                Glide.with(context)
                    .load(mediaPath)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .placeholder(R.drawable.img_image_placeholder)
                    .dontTransform()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivReplyImage)
            } else {
                tvReplyMedia.visibility = View.VISIBLE
                ivReplyImage.visibility = View.GONE
            }
        }
}
