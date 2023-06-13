package com.clover.studio.exampleapp.utils.helpers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.MessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.MessageRecords
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.ui.main.chat.ChatAdapter
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.google.android.material.imageview.ShapeableImageView
import com.vanniktech.emoji.EmojiTextView

const val MAX_REACTIONS = 3
private const val TEXT_SIZE_BIG = 11
private const val TEXT_SIZE_SMALL = 5
const val MAX_WIDTH = 256
const val MAX_HEIGHT = 300

object ChatAdapterHelper {

    /**A method that sets all views' visibility to Gone except the active one
     * @param viewToShow - active view
     * @param holder -SentMessageHolder / ReceivedMessageHolder */
    fun setViewsVisibility(viewToShow: View, holder: RecyclerView.ViewHolder) {
        val viewsToHide = listOf<View>(
            holder.itemView.findViewById<TextView>(R.id.tv_message),
            holder.itemView.findViewById<CardView>(R.id.cl_image_chat),
            holder.itemView.findViewById<ConstraintLayout>(R.id.file_layout),
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

    /** A method that loads a media item into Glide
     * @param context - Context
     * @param mediaPath - Path of media item
     * @param imageView - ImageView where we want to load the image
     * */
    fun loadMedia(context: Context, mediaPath: String, imageView: ImageView) {
        Glide.with(context)
            .load(mediaPath)
            .override(MAX_WIDTH, MAX_HEIGHT)
            .dontTransform()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }

    /** A method that displays reactions
     * @param chatMessage - A message that contains message records and for which we show reactions
     * @param tvReactedEmoji - Text view in which reactions and the number of reactions are recorded
     * @param cvReactedEmoji - Card view that shows / hides reactions*/
    fun bindReactions(
        chatMessage: MessageAndRecords?,
        tvReactedEmoji: TextView,
        cvReactedEmoji: CardView
    ) {
        val reactionList = chatMessage!!.records!!.sortedByDescending { it.createdAt }
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

    /** A method that manipulates base reactions
     * @param reactionList - list of reactions that need to be filtered according to certain rules
    Rules:
    - there can be a maximum of 3 reactions
    - if there are more, then the total number is displayed next to it
    - if there are a total of three reactions and 2 of those 3 are equal, then two reactions and the
    total number are shown next to (3)
    - if there are less than three reactions, then both are shown
    - reactions are placed in the order in which they are placed, which means that the newer
    reactions will push out the older ones if there are more than three of them
    - the most recent reaction is on the left (first)
     */
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

    /** A method that displays reply messages for all types of message types */
    fun bindReply(
        context: Context,
        users: List<User>,
        chatMessage: MessageAndRecords,
        ivReplyImage: ShapeableImageView,
        tvReplyMedia: TextView,
        tvMessageReply: EmojiTextView,
        clReplyMessage: ConstraintLayout,
        clContainer: ConstraintLayout,
        tvUsername: TextView,
        sender: Boolean,
    ) {
        ivReplyImage.visibility = View.GONE
        tvReplyMedia.visibility = View.GONE
        tvMessageReply.visibility = View.GONE

        val params =
            clReplyMessage.layoutParams as ConstraintLayout.LayoutParams
        params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        val original = chatMessage.message.body?.text?.length
        clReplyMessage.visibility = View.VISIBLE
        val username = tvUsername.text.length

        if (sender) {
            clContainer.setBackgroundResource(R.drawable.bg_message_send)
        } else {
            clContainer.setBackgroundResource(R.drawable.bg_message_received)
        }

        tvUsername.text =
            users.firstOrNull { it.id == chatMessage.message.body?.referenceMessage?.fromUserId }!!.formattedDisplayName

        when (chatMessage.message.body?.referenceMessage?.type) {
            /**Image or video type*/
            Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE -> {
                if (original!! >= TEXT_SIZE_BIG) {
                    params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                }

//                val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//                val imagePath = "$directory/${chatMessage.message.localId}.${Const.FileExtensions.JPG}"

                val imagePath =
                    chatMessage.message.body.referenceMessage?.body?.thumbId?.let { imagePath ->
                        Tools.getFilePathUrl(
                            imagePath
                        )
                    }
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

                tvMessageReply.visibility = View.GONE
                ivReplyImage.visibility = View.VISIBLE
                tvReplyMedia.visibility = View.VISIBLE

                loadMedia(
                    context,
                    imagePath!!,
                    ivReplyImage,
                )
            }
            /** Audio type */
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
            /** File type */
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
            /** Text type */
            else -> {
                tvMessageReply.visibility = View.VISIBLE
                ivReplyImage.visibility = View.GONE
                tvReplyMedia.visibility = View.GONE

                val replyText = chatMessage.message.body?.referenceMessage?.body?.text
                tvMessageReply.text = replyText
                val reply = replyText?.length

                if (original != null && reply != null) {
                    if (original >= reply && original >= TEXT_SIZE_SMALL && username < original) {
                        params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                    }
                }
            }
        }
    }

    /** The method that displays the status of the message for the sender only - sending, sent, delivered */
    fun showMessageStatus(
        chatMessage: MessageAndRecords?,
        ivMessageStatus: ImageView
    ) {

        val message = chatMessage?.message
        when {
            message?.totalUserCount == 0 -> {
                ivMessageStatus.setImageResource(R.drawable.img_clock)
            }

            message?.totalUserCount == message?.seenCount -> {
                ivMessageStatus.setImageResource(R.drawable.img_seen)
            }

            message?.totalUserCount == message?.deliveredCount -> {
                ivMessageStatus.setImageResource(R.drawable.img_done)
            }

            message?.deliveredCount != null && message.deliveredCount >= 0 -> {
                ivMessageStatus.setImageResource(R.drawable.img_sent)
            }

            message?.deliveredCount == -1 -> {
                ivMessageStatus.setImageResource(R.drawable.img_alert)
            }
        }
    }

    /** A method that displays a file icon depending on the file type */
    fun addFiles(context: Context, ivFileType: ImageView, fileExtension: String) {
        val drawableResId = when (fileExtension) {
            Const.FileExtensions.PDF -> R.drawable.img_pdf_black
            Const.FileExtensions.ZIP, Const.FileExtensions.RAR -> R.drawable.img_folder_zip
            Const.FileExtensions.MP3, Const.FileExtensions.WAW -> R.drawable.img_audio_file
            else -> R.drawable.img_file_black
        }

        ivFileType.setImageDrawable(
            ResourcesCompat.getDrawable(
                context.resources,
                drawableResId,
                null
            )
        )
    }

    /** A method that shows/does not show the name and picture of another user if the messages are
     *  sent in sequence
     *  @param position - Position of current message
     *  @param holder - ReceivedMessageHolder
     *  @param currentList - Currently displayed list
     * */
    fun showHideUserInformation(
        position: Int,
        holder: ChatAdapter.ReceivedMessageHolder,
        currentList: MutableList<MessageAndRecords>,
    ) {
        if (position > 0) {
            try {
                val nextItem = currentList[position + 1].message.fromUserId
                val previousItem = currentList[position - 1].message.fromUserId

                val currentItem = currentList[position].message.fromUserId

                if (previousItem == currentItem) {
                    holder.binding.ivUserImage.visibility = View.INVISIBLE
                } else {
                    holder.binding.ivUserImage.visibility = View.VISIBLE
                }

                if (nextItem == currentItem) {
                    holder.binding.tvUsername.visibility = View.GONE
                } else {
                    holder.binding.tvUsername.visibility = View.VISIBLE
                }
            } catch (ex: IndexOutOfBoundsException) {
                Tools.checkError(ex)
                holder.binding.tvUsername.visibility = View.VISIBLE
                holder.binding.ivUserImage.visibility = View.VISIBLE
            }
        } else {
            holder.binding.tvUsername.visibility = View.VISIBLE
            holder.binding.ivUserImage.visibility = View.VISIBLE
        }
    }

    fun getFileMimeType(context: Context?, uri: Uri): String? {
        val cR: ContentResolver = context!!.contentResolver
        return cR.getType(uri)
    }
}
