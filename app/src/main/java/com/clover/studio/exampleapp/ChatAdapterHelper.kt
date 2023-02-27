package com.clover.studio.exampleapp

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.MessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.MessageRecords
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.google.android.material.imageview.ShapeableImageView
import com.vanniktech.emoji.EmojiTextView

const val MAX_REACTIONS = 3
private const val TEXT_SIZE_BIG = 11
private const val TEXT_SIZE_SMALL = 5

object ChatAdapterHelper {

    fun setViewsVisibility(viewToShow: View, holder: RecyclerView.ViewHolder) {
        val viewsToHide = listOf<View>(
            holder.itemView.findViewById<TextView>(R.id.tv_message),
            holder.itemView.findViewById<CardView>(R.id.cv_image),
            holder.itemView.findViewById<ConstraintLayout>(R.id.cl_file_message),
            holder.itemView.findViewById<ConstraintLayout>(R.id.cl_videos),
            holder.itemView.findViewById<CardView>(R.id.cv_audio),
            holder.itemView.findViewById<ConstraintLayout>(R.id.cl_reply_message)
        )

        for (view in viewsToHide) {
            if (view == viewToShow) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }
    }

    fun bindReactions(
        it: MessageAndRecords?,
        tvReactedEmoji: TextView,
        cvReactedEmoji: CardView
    ) {
        val reactionList = it!!.records!!.sortedByDescending { it.createdAt }
        val reactionText = getDatabaseReaction(reactionList)

        // Show reactions if there are any in the database
        if (reactionText.isNotEmpty()) {
            if (reactionText.last().isDigit()) {
                // If last char is number - resize it
                val spanStringBuilder = SpannableStringBuilder(reactionText)
                spanStringBuilder.setSpan(
                    RelativeSizeSpan(0.5f),
                    reactionText.length - 2,
                    reactionText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                tvReactedEmoji.text = spanStringBuilder.append(" ")
            } else {
                tvReactedEmoji.text = reactionText
            }
            cvReactedEmoji.visibility = View.VISIBLE
        } else {
            cvReactedEmoji.visibility = View.GONE
        }
    }

    private fun getDatabaseReaction(
        reactionList: List<MessageRecords>?
    ): String {
        // This list contains only reaction types.
        // Before we filter the list to get unique reaction values, we need the total number of reactions for if conditions.
        val tmp: MutableList<MessageRecords> =
            reactionList!!.filter { it.type == Const.JsonFields.REACTION }.toMutableList()
        val total = tmp.count()
        // We remove duplicate reactions from the first list.
        var filteredList = tmp.distinctBy { it.reaction }.toMutableList()

        var reactionText = ""
        val totalText: String

        if (filteredList.isNotEmpty()) {
            // If the list is longer than three reactions, show only the first three reactions.
            if (filteredList.size > MAX_REACTIONS) {
                filteredList = filteredList.subList(0, MAX_REACTIONS)
                totalText = total.toString()
            } else {
                totalText = if (filteredList.size == 1 && total > 1) {
                    total.toString()
                } else {
                    ""
                }
            }
            for (reaction in filteredList) {
                reactionText += reaction.reaction + " "
            }
            reactionText += totalText
        }
        return reactionText.trim()
    }

    fun bindReply(
        context: Context,
        users: List<User>,
        chatMessage: MessageAndRecords,
        ivReplyImage: ShapeableImageView,
        tvReplyMedia: TextView,
        tvMessageReply: EmojiTextView,
        clReplyMessage: ConstraintLayout,
        clContainer: ConstraintLayout,
        tvUsername: TextView
    ) {
        ivReplyImage.visibility = View.GONE
        tvReplyMedia.visibility = View.GONE
        tvMessageReply.visibility = View.GONE
        if (chatMessage.message.replyId != null && chatMessage.message.replyId != 0L) {
            val params =
                clReplyMessage.layoutParams as ConstraintLayout.LayoutParams
            params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            val original = chatMessage.message.body?.text?.length
            clReplyMessage.visibility = View.VISIBLE
            clContainer.setBackgroundResource(R.drawable.bg_message_user)
            for (roomUser in users) {
                if (chatMessage.message.body?.referenceMessage?.fromUserId == roomUser.id) {
                    tvUsername.text = roomUser.displayName
                    break
                }
            }
            when (chatMessage.message.body?.referenceMessage?.type) {
                Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE -> {
                    if (original!! >= TEXT_SIZE_BIG) {
                        params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                    }
                    val imagePath =
                        chatMessage.message.body.referenceMessage?.body?.thumbId?.let { imagePath ->
                            Tools.getFilePathUrl(
                                imagePath
                            )
                        }
                    tvMessageReply.visibility = View.GONE
                    ivReplyImage.visibility = View.VISIBLE
                    tvReplyMedia.visibility = View.VISIBLE
                    if (chatMessage.message.body.referenceMessage?.type == Const.JsonFields.IMAGE_TYPE) {
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
                    Glide.with(context)
                        .load(imagePath)
                        .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .placeholder(R.drawable.img_image_placeholder)
                        .dontTransform()
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivReplyImage)
                }
                Const.JsonFields.AUDIO_TYPE -> {
                    if (original!! >= TEXT_SIZE_SMALL) {
                        params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                    }
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
                Const.JsonFields.FILE_TYPE -> {
                    if (original!! >= TEXT_SIZE_SMALL) {
                        params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                    }
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
                else -> {
                    tvMessageReply.visibility = View.VISIBLE
                    ivReplyImage.visibility = View.GONE
                    tvReplyMedia.visibility = View.GONE
                    val replyText = chatMessage.message.body?.referenceMessage?.body?.text
                    tvMessageReply.text = replyText

                    // Check which layout is wider
                    val reply = replyText?.length
                    if (original != null && reply != null) {
                        if (original >= reply && original >= TEXT_SIZE_SMALL) {
                            params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                        }
                    }
                }
            }
        }
    }

    fun showMessageStatus(
        context: Context,
        chatMessage: MessageAndRecords?,
        ivMessageStatus: ImageView
    ) {
        if (chatMessage!!.message.totalUserCount != 0) {
            if (chatMessage.message.totalUserCount == chatMessage.message.seenCount) {
                ivMessageStatus.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.img_seen
                    )
                )
            } else if (chatMessage.message.totalUserCount == chatMessage.message.deliveredCount) {
                ivMessageStatus.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.img_done
                    )
                )
            } else if (chatMessage.message.deliveredCount!! >= 0) {
                ivMessageStatus.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.img_sent
                    )
                )
            }
        } else {
            ivMessageStatus.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.img_clock
                )
            )
        }
    }

    fun addFiles(context: Context, message: Message, ivFileType: ImageView) {
        when (message.body?.file?.fileName?.substringAfterLast(".")) {
            Const.FileExtensions.PDF -> ivFileType.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.img_pdf_black,
                    null
                )
            )
            Const.FileExtensions.ZIP, Const.FileExtensions.RAR -> ivFileType.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.img_folder_zip,
                    null
                )
            )
            Const.FileExtensions.MP3, Const.FileExtensions.WAW -> ivFileType.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.img_audio_file,
                    null
                )
            )
            else -> ivFileType.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.img_file_black,
                    null
                )
            )
        }
    }
}