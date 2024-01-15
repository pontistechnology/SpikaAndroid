package com.clover.studio.spikamessenger.utils.helpers

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.ui.main.chat.ChatAdapter
import com.clover.studio.spikamessenger.utils.Const
import timber.log.Timber

const val MAX_REACTIONS = 3

object ChatAdapterHelper {

    /**A method that sets all views' visibility to Gone except the active one
     * @param viewToShow - active view
     * @param holder -SentMessageHolder / ReceivedMessageHolder */
    fun setViewsVisibility(viewToShow: View, holder: RecyclerView.ViewHolder) {
        val viewsToHide = listOf<View>(
            holder.itemView.findViewById<TextView>(R.id.tv_message),
            holder.itemView.findViewById<CardView>(R.id.cv_media),
            holder.itemView.findViewById<FrameLayout>(R.id.fl_reply_msg_container),
        )

        viewsToHide.forEach {
            it.visibility = if (it == viewToShow) View.VISIBLE else View.GONE
        }

        val containerIds = listOf(R.id.fl_reply_msg_container, R.id.fl_media_container)
        containerIds.forEach {
            holder.itemView.findViewById<FrameLayout>(it).removeAllViews()
        }
    }

    /** A method that loads a media item into Glide
     * @param context - Context
     * @param mediaPath - Path of media item
     * @param mediaImage - ImageView where we want to load the image
     * */
    fun loadMedia(
        context: Context,
        mediaPath: String,
        mediaImage: ImageView,
        loadingImage: ImageView?,
        height: Int,
        width: Int,
        playButton: ImageView?
    ) {
        val params = mediaImage.layoutParams
        params.height = convertToDp(context, height)
        params.width = convertToDp(context, width)
        mediaImage.layoutParams = params

        var rotationAnimator: ObjectAnimator? = null
        if (loadingImage != null && loadingImage.visibility == View.VISIBLE) {
            rotationAnimator = ObjectAnimator.ofFloat(loadingImage, "rotation", 0f, 360f)
            rotationAnimator.apply {
                repeatCount = ObjectAnimator.INFINITE
                duration = 2000
                interpolator = LinearInterpolator()
                start()
            }
        }

        Glide.with(context)
            .load(mediaPath)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Timber.d("Load Failed")
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    loadingImage?.visibility = View.GONE
                    mediaImage.visibility = View.VISIBLE
                    if (playButton != null) {
                        playButton.visibility = View.VISIBLE
                    }
                    rotationAnimator?.end()
                    return false
                }
            })
            .into(mediaImage)
    }

    private fun convertToDp(context: Context, dp: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
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

    /** The method that displays the status of the message for the sender only - sending, sent, delivered */
    fun showMessageStatus(
        chatMessage: MessageAndRecords?,
        ivMessageStatus: ImageView
    ) {
        val message = chatMessage?.message
        when (message?.messageStatus) {
            Resource.Status.ERROR.toString() -> {
                ivMessageStatus.setImageResource(R.drawable.img_alert)
            }

            Resource.Status.LOADING.toString() -> {
                ivMessageStatus.setImageResource(R.drawable.img_clock)
            }

            Resource.Status.SUCCESS.toString(), null -> {
                if (message?.totalUserCount == message?.seenCount) {
                    ivMessageStatus.setImageResource(R.drawable.img_seen)
                } else if (message?.totalUserCount == message?.deliveredCount) {
                    ivMessageStatus.setImageResource(R.drawable.img_done)
                } else if (message?.deliveredCount != null && message.deliveredCount >= 0) {
                    ivMessageStatus.setImageResource(R.drawable.img_sent)
                }
            }
        }
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
        try {
            val currentMessage = currentList[position].message
            val nextMessage =
                if (position + 1 < currentList.size) currentList[position + 1].message else null
            val previousMessage = if (position - 1 >= 0) currentList[position - 1].message else null

            if (Const.JsonFields.SYSTEM_TYPE == currentMessage.type) {
                // For system messages, don't display username or user image
                holder.binding.tvUsername.visibility = View.GONE
                holder.binding.ivUserImage.visibility = View.GONE
            } else {
                // Regular user messages handling
                if (position == 0 || (previousMessage != null && (previousMessage.fromUserId != currentMessage.fromUserId || Const.JsonFields.SYSTEM_TYPE == previousMessage.type))) {
                    holder.binding.ivUserImage.visibility = View.VISIBLE
                } else {
                    holder.binding.ivUserImage.visibility = View.INVISIBLE
                }

                if (nextMessage != null && nextMessage.fromUserId == currentMessage.fromUserId && Const.JsonFields.SYSTEM_TYPE != nextMessage.type) {
                    holder.binding.tvUsername.visibility = View.GONE
                } else {
                    holder.binding.tvUsername.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Timber.d("Exception: $e")
        }
    }

    fun formatSystemMessage(
        context: Context,
        subjectName: String,
        objectNames: String?,
        systemObject: String,
        type: String
    ): SpannableString {
        val text: SpannableString

        val spannableSubject = SpannableString(subjectName).applyBold()

        val spannableObjectNames = objectNames?.let {
            SpannableString(it).applyBold()
        }

        val spannableSystemObject = SpannableString(systemObject).applyBold()

        text = when (type) {
            // Subject + action + object
            Const.SystemMessages.UPDATED_NOTE, Const.SystemMessages.DELETED_NOTE,
            Const.SystemMessages.CREATED_NOTE, Const.SystemMessages.CREATED_GROUP,
            Const.SystemMessages.UPDATED_GROUP_NAME, Const.SystemMessages.UPDATED_GROUP_MEMBERS ->
                SpannableString(
                    "$spannableSubject ${
                        getAction(
                            context = context,
                            type = type
                        )
                    } $spannableSystemObject"
                )

            // Subject + action
            Const.SystemMessages.UPDATED_GROUP_AVATAR, Const.SystemMessages.USER_LEFT_GROUP,
            Const.SystemMessages.UPDATED_GROUP, Const.SystemMessages.UPDATED_GROUP_ADMINS -> SpannableString(
                "$spannableSubject ${
                    getAction(
                        context = context,
                        type = type
                    )
                }"
            )

            // Subject + action + object + additional action
            Const.SystemMessages.REMOVED_GROUP_MEMBERS -> SpannableString(
                "$spannableSubject ${
                    getAction(
                        context = context,
                        type = type
                    )
                } $spannableObjectNames ${context.getString(R.string.from_the_group)}"
            )

            Const.SystemMessages.ADDED_GROUP_MEMBERS -> SpannableString(
                "$spannableSubject ${
                    getAction(
                        context = context,
                        type = type
                    )
                } $spannableObjectNames ${context.getString(R.string.to_the_group)}"
            )

            Const.SystemMessages.ADDED_GROUP_ADMINS, Const.SystemMessages.REMOVED_GROUP_ADMINS -> SpannableString(
                "$spannableSubject ${
                    getAction(
                        context = context,
                        type = type
                    )
                } $spannableObjectNames ${context.getString(R.string.as_group_admin)}"
            )

            else -> SpannableString("Error")
        }

        return text
    }

    private fun CharSequence.applyBold(): SpannableString {
        val spannable = SpannableString(this)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }


    private fun getAction(context: Context, type: String): String {
        return when (type) {
            // Notes
            Const.SystemMessages.UPDATED_NOTE -> context.getString(R.string.updated_note)
            Const.SystemMessages.CREATED_NOTE -> context.getString(R.string.created_note)
            Const.SystemMessages.DELETED_NOTE -> context.getString(R.string.deleted_note)

            // Groups
            Const.SystemMessages.UPDATED_GROUP -> context.getString(R.string.updated_group)
            Const.SystemMessages.UPDATED_GROUP_NAME -> context.getString(R.string.updated_group_name)
            Const.SystemMessages.UPDATED_GROUP_AVATAR -> context.getString(R.string.updated_group_avatar)
            Const.SystemMessages.USER_LEFT_GROUP -> context.getString(R.string.user_left_group)

            // Users
            Const.SystemMessages.ADDED_GROUP_ADMINS -> context.getString(R.string.added_group_admins)
            Const.SystemMessages.ADDED_GROUP_MEMBERS -> context.getString(R.string.added_group_members)
            Const.SystemMessages.REMOVED_GROUP_ADMINS, Const.SystemMessages.REMOVED_GROUP_MEMBERS ->
                context.getString(R.string.removed_group_admins)

            Const.SystemMessages.UPDATED_GROUP_ADMINS -> context.getString(R.string.updated_group_admins)
            Const.SystemMessages.UPDATED_GROUP_MEMBERS -> context.getString(R.string.updated_group_members)

            else -> context.getString(R.string.error)
        }
    }

}
