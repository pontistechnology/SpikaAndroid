package com.clover.studio.spikamessenger.ui.main.chat.chat_layouts

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.databinding.ReplyLayoutBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.helpers.ColorHelper
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel


class ReplyLayout(context: Context) :
    ConstraintLayout(context) {

    private var bindingSetup: ReplyLayoutBinding = ReplyLayoutBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup
    private var listener: ReplyLayoutListener? = null

    interface ReplyLayoutListener {
        fun replyLayoutClick()
    }

    fun setReplyLayoutListener(listener: ReplyLayoutListener?) {
        this.listener = listener
    }

    fun bindReply(
        context: Context,
        users: List<User>,
        chatMessage: MessageAndRecords,
        clContainer: ConstraintLayout,
        sender: Boolean,
        roomType: String?
    ) = with(binding) {

        ivReplyImage.visibility = View.GONE
        tvReplyMedia.visibility = View.GONE
        tvMessageReply.visibility = View.GONE

        clReplyMessage.visibility = View.VISIBLE

        clReplyMessage.setOnClickListener {
            listener?.replyLayoutClick()
        }

        if (sender) {
            clContainer.setBackgroundResource(R.drawable.bg_message_send)
            clReplyMessage.setBackgroundResource(R.drawable.bg_message_send)
            clReplyMessage.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getSecondaryColor(context))
        } else {
            clContainer.setBackgroundResource(R.drawable.bg_message_received)
            clReplyMessage.setBackgroundResource(R.drawable.bg_message_received)
            clReplyMessage.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getPrimaryColor(context))
        }

        if (roomType == Const.JsonFields.PRIVATE) {
            tvUsernameOther.visibility = View.GONE
        } else {
            tvUsernameOther.visibility = View.VISIBLE
            tvUsernameOther.text =
                users.firstOrNull { it.id == chatMessage.message.referenceMessage?.fromUserId }?.formattedDisplayName
        }

        when (chatMessage.message.referenceMessage?.type) {
            /**Image or video type*/
            Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE -> {
                if (chatMessage.message.referenceMessage?.type == Const.JsonFields.IMAGE_TYPE) {
                    tvReplyMedia.text = context.getString(
                        R.string.media,
                        context.getString(R.string.photo)
                    )
                    tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.img_camera_reply,
                        0,
                        0,
                        0
                    )
                } else {
                    tvReplyMedia.text = context.getString(
                        R.string.media,
                        context.getString(R.string.video)
                    )
                    tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.img_video_reply,
                        0,
                        0,
                        0
                    )
                }

                tvMessageReply.visibility = View.GONE
                ivReplyImage.visibility = View.VISIBLE
                tvReplyMedia.visibility = View.VISIBLE

                val mediaPath = chatMessage.message.referenceMessage?.body?.fileId?.let {
                    Tools.getFilePathUrl(it)
                }

                Glide.with(context)
                    .load(mediaPath)
                    .into(ivReplyImage)

                setAppearanceModel(ivReplyImage, sender)
            }
            /** Audio type */
            Const.JsonFields.AUDIO_TYPE -> {
                tvMessageReply.visibility = View.GONE
                ivReplyImage.visibility = View.GONE
                tvReplyMedia.visibility = View.VISIBLE
                tvReplyMedia.text =
                    context.getString(R.string.media, context.getString(R.string.audio))
                tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.img_audio_reply,
                    0,
                    0,
                    0
                )
            }
            /** File type */
            Const.JsonFields.FILE_TYPE -> {
                tvMessageReply.visibility = View.GONE
                tvReplyMedia.visibility = View.VISIBLE
                tvReplyMedia.text =
                    context.getString(R.string.media, context.getString(R.string.file))
                tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.img_file_reply,
                    0,
                    0,
                    0
                )
            }
            /** Text type */
            else -> {
                tvMessageReply.visibility = View.VISIBLE
                ivReplyImage.visibility = View.GONE
                tvReplyMedia.visibility = View.GONE

                tvMessageReply.text = chatMessage.message.referenceMessage?.body?.text
            }
        }
    }

    private fun setAppearanceModel(ivReplyImage: ShapeableImageView, sender: Boolean) {
        val builder = ShapeAppearanceModel().toBuilder()

        if (sender) {
            builder.setTopRightCorner(
                CornerFamily.ROUNDED,
                resources.getDimension(R.dimen.eight_dp_margin)
            )
        } else {
            builder.setTopRightCorner(
                CornerFamily.ROUNDED,
                resources.getDimension(R.dimen.eight_dp_margin)
            )
                .setBottomRightCorner(
                    CornerFamily.ROUNDED,
                    resources.getDimension(R.dimen.eight_dp_margin)
                )
        }

        ivReplyImage.shapeAppearanceModel = builder.build()
    }
}
