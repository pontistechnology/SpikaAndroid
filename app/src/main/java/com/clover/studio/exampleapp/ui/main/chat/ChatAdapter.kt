package com.clover.studio.exampleapp.ui.main.chat

import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.MessageAndRecords
import com.clover.studio.exampleapp.data.models.ReactionMessage
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.ItemMessageMeBinding
import com.clover.studio.exampleapp.databinding.ItemMessageOtherBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.Tools.getRelativeTimeSpan
import timber.log.Timber
import java.util.*


private const val VIEW_TYPE_MESSAGE_SENT = 1
private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
private var REACTION = ""
private var reactionMessage: ReactionMessage = ReactionMessage("", 0)

class ChatAdapter(
    private val context: Context,
    private val myUserId: Int,
    private val users: List<User>,
    private val addReaction: ((reaction: ReactionMessage) -> Unit),
) :
    ListAdapter<MessageAndRecords, RecyclerView.ViewHolder>(MessageAndRecordsDiffCallback()) {

    inner class SentMessageHolder(val binding: ItemMessageMeBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class ReceivedMessageHolder(val binding: ItemMessageOtherBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            val binding =
                ItemMessageMeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentMessageHolder(binding)
        } else {
            val binding =
                ItemMessageOtherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedMessageHolder(binding)
        }
    }


    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        return if (message.message.fromUserId == myUserId) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position).let { it ->

            Timber.d("messageRecords: $it")

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it.message.createdAt!!
            val date = calendar.get(Calendar.DAY_OF_MONTH)

            // View holder for my messages
            // TODO can two view holders use same method for binding if all views are the same?
            if (holder.itemViewType == VIEW_TYPE_MESSAGE_SENT) {
                holder as SentMessageHolder
                when (it.message.type) {
                    Const.JsonFields.TEXT -> {
                        holder.binding.tvMessage.text = it.message.body?.text
                        holder.binding.tvMessage.visibility = View.VISIBLE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                    }
                    Const.JsonFields.CHAT_IMAGE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.VISIBLE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE

                        val imagePath = it.message.body?.file?.path?.let { imagePath ->
                            Tools.getFileUrl(
                                imagePath
                            )
                        }

                        Glide.with(context)
                            .load(imagePath)
                            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                            .placeholder(R.drawable.ic_baseline_image_24)
                            .dontTransform()
                            .dontAnimate()
                            .into(holder.binding.ivChatImage)

                        holder.binding.ivChatImage.setOnClickListener { view ->
                            val action =
                                ChatMessagesFragmentDirections.actionChatMessagesFragment2ToVideoFragment2(
                                    "", imagePath!!
                                )
                            view.findNavController().navigate(action)
                        }
                    }

                    Const.JsonFields.FILE_TYPE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.VISIBLE
                        holder.binding.clVideos.visibility = View.GONE

                        holder.binding.tvFileTitle.text = it.message.body?.file?.fileName
                        val megabyteText =
                            "${
                                Tools.calculateToMegabyte(it.message.body?.file?.size!!)
                                    .toString()
                            } ${holder.itemView.context.getString(R.string.files_mb_text)}"
                        holder.binding.tvFileSize.text = megabyteText
                        addFiles(it.message, holder.binding.ivFileType)

                        // TODO implement file handling when clicked on in chat
                        val filePath = it.message.body.file?.path?.let { filePath ->
                            Tools.getFileUrl(
                                filePath
                            )
                        }
                        holder.binding.tvFileTitle.setOnClickListener {

                        }
                    }

                    Const.JsonFields.VIDEO -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE

                        val videoPath = it.message.body?.file?.path?.let { videoPath ->
                            Tools.getFileUrl(
                                videoPath
                            )
                        }

                        Glide.with(context)
                            .load(videoPath)
                            .priority(Priority.HIGH)
                            .dontTransform()
                            .dontAnimate()
                            .placeholder(R.drawable.ic_baseline_videocam_24)
                            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                            .into(holder.binding.ivVideoThumbnail)

                        holder.binding.clVideos.visibility = View.VISIBLE
                        holder.binding.ivPlayButton.setImageResource(R.drawable.ic_baseline_play_circle_filled_24)

                        holder.binding.ivPlayButton.setOnClickListener { view ->
                            val action =
                                ChatMessagesFragmentDirections.actionChatMessagesFragment2ToVideoFragment2(
                                    videoPath!!, ""
                                )
                            view.findNavController().navigate(action)
                        }
                    }

                    else -> {
                        holder.binding.tvMessage.visibility = View.VISIBLE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                    }
                }

                // Reactions section:
                it.records?.forEach { records ->
                    // TODO - Duplicated reactions on scroll
                    if (!records.reaction.isNullOrEmpty()) {
                        val text = records.reaction
                        holder.binding.tvReactedEmoji.text = text
                        holder.binding.cvReactedEmoji.visibility = View.VISIBLE
                    }
                }

                holder.binding.clContainer.setOnLongClickListener { _ ->
                    holder.binding.cvReactions.visibility = View.VISIBLE
                    holder.binding.cvMessageOptions.visibility = View.VISIBLE
                    listeners(holder, it.message.id)
                    true
                }


                showDateHeader(position, date, holder.binding.tvSectionHeader, it.message)

                when {
                    it.message.seenCount!! > 0 -> {
                        holder.binding.ivMessageStatus.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.img_seen
                            )
                        )
                    }
                    it.message.totalUserCount == it.message.deliveredCount -> {
                        holder.binding.ivMessageStatus.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.img_done
                            )
                        )
                    }
                    it.message.deliveredCount!! >= 0 -> {
                        holder.binding.ivMessageStatus.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.img_sent
                            )
                        )
                    }
                    else -> {
                        holder.binding.ivMessageStatus.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.img_clock
                            )
                        )
                    }
                }
            } else {
                // View holder for messages from other users
                holder as ReceivedMessageHolder
                when (it.message.type) {
                    Const.JsonFields.TEXT -> {
                        holder.binding.tvMessage.text = it.message.body?.text
                        holder.binding.tvMessage.visibility = View.VISIBLE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                    }
                    Const.JsonFields.CHAT_IMAGE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.clImages.visibility = View.VISIBLE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE

                        val imagePath = it.message.body?.file?.path?.let { imagePath ->
                            Tools.getFileUrl(
                                imagePath
                            )
                        }

                        Glide.with(context)
                            .load(imagePath)
                            .placeholder(R.drawable.ic_baseline_image_24)
                            .dontTransform()
                            .dontAnimate()
                            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                            .into(holder.binding.ivChatImage)


                        holder.binding.ivChatImage.setOnClickListener { view ->
                            val action =
                                ChatMessagesFragmentDirections.actionChatMessagesFragment2ToVideoFragment2(
                                    "", imagePath!!
                                )
                            view.findNavController().navigate(action)
                        }
                    }


                    Const.JsonFields.VIDEO -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.clImages.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.VISIBLE

                        val videoPath = it.message.body?.file?.path?.let { videoPath ->
                            Tools.getFileUrl(
                                videoPath
                            )
                        }

                        Glide.with(context)
                            .load(videoPath)
                            .priority(Priority.HIGH)
                            .dontTransform()
                            .dontAnimate()
                            .placeholder(R.drawable.ic_baseline_videocam_24)
                            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                            .into(holder.binding.ivVideoThumbnail)

                        holder.binding.clVideos.visibility = View.VISIBLE
                        holder.binding.ivPlayButton.setImageResource(R.drawable.ic_baseline_play_circle_filled_24)

                        holder.binding.ivPlayButton.setOnClickListener { view ->
                            val action =
                                ChatMessagesFragmentDirections.actionChatMessagesFragment2ToVideoFragment2(
                                    videoPath!!, ""
                                )
                            view.findNavController().navigate(action)
                        }
                    }


                    Const.JsonFields.FILE_TYPE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.VISIBLE
                        holder.binding.clVideos.visibility = View.GONE

                        holder.binding.tvFileTitle.text = it.message.body?.file?.fileName
                        val megabyteText =
                            "${
                                Tools.calculateToMegabyte(it.message.body?.file?.size!!)
                                    .toString()
                            } ${holder.itemView.context.getString(R.string.files_mb_text)}"
                        holder.binding.tvFileSize.text = megabyteText

                        addFiles(it.message, holder.binding.ivFileType)
                    }
                    else -> {
                        holder.binding.tvMessage.visibility = View.VISIBLE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                    }
                }

                if (it.message.body?.text.isNullOrEmpty()) {
                    holder.binding.tvMessage.visibility = View.GONE
                    holder.binding.cvImage.visibility = View.VISIBLE

                    Glide.with(context)
                        .load(it.message.body?.file?.path?.let { imagePath ->
                            Tools.getFileUrl(
                                imagePath
                            )
                        })
                        .into(holder.binding.ivChatImage)
                } else {
                    holder.binding.tvMessage.visibility = View.VISIBLE
                    holder.binding.cvImage.visibility = View.GONE
                }

                for (roomUser in users) {
                    if (it.message.fromUserId == roomUser.id) {
                        holder.binding.tvUsername.text = roomUser.displayName
                        Glide.with(context)
                            .load(roomUser.avatarUrl?.let { avatarUrl ->
                                Tools.getFileUrl(
                                    avatarUrl
                                )
                            })
                            .placeholder(context.getDrawable(R.drawable.img_user_placeholder))
                            .into(holder.binding.ivUserImage)
                        break
                    }
                }

                // Reactions section:
                it.records?.forEach { records ->
                    // TODO - Duplicated reactions on scroll
                    if (it.message.id == records.messageId) {
                        if (!records.reaction.isNullOrEmpty()) {
                            val text = records.reaction
                            holder.binding.tvReactedEmoji.text = text
                            holder.binding.cvReactedEmoji.visibility = View.VISIBLE
                        } else {
                            holder.binding.cvReactedEmoji.visibility = View.GONE
                        }
                    }
                }

                holder.binding.clContainer.setOnLongClickListener { _ ->
                    holder.binding.cvReactions.visibility = View.VISIBLE
                    holder.binding.cvMessageOptions.visibility = View.VISIBLE
                    listeners(holder, it.message.id)
                    true
                }

                showDateHeader(position, date, holder.binding.tvSectionHeader, it.message)

                if (position > 0) {
                    try {
                        val nextItem = getItem(position + 1).fromUserId
                        val previousItem = getItem(position - 1).fromUserId

                        val currentItem = it.message.fromUserId
                        Timber.d("Items : $nextItem, $currentItem ${nextItem == currentItem}")

                        if (previousItem == currentItem) {
                            holder.binding.cvUserImage.visibility = View.INVISIBLE
                        } else {
                            holder.binding.cvUserImage.visibility = View.VISIBLE
                        }

                        if (nextItem == currentItem) {
                            holder.binding.tvUsername.visibility = View.GONE
                        } else {
                            holder.binding.tvUsername.visibility = View.VISIBLE
                        }
                    } catch (ex: IndexOutOfBoundsException) {
                        Tools.checkError(ex)
                        holder.binding.tvUsername.visibility = View.VISIBLE
                        holder.binding.cvUserImage.visibility = View.VISIBLE
                    }
                } else {
                    holder.binding.tvUsername.visibility = View.VISIBLE
                    holder.binding.cvUserImage.visibility = View.VISIBLE
                }
            }
        }
    }

    // TODO - same listeners method for both holders
    private fun listeners(holder: ReceivedMessageHolder, messageId: Int) {
        holder.binding.reactions.clEmoji.children.forEach { child ->
            child.setOnClickListener {
                when (child) {
                    holder.binding.reactions.tvThumbsUpEmoji -> {
                        REACTION = context.getString(R.string.thumbs_up_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                    holder.binding.reactions.tvHeartEmoji -> {
                        REACTION = context.getString(R.string.heart_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                    holder.binding.reactions.tvCryingEmoji -> {
                        REACTION = context.getString(R.string.crying_face_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                    holder.binding.reactions.tvAstonishedEmoji -> {
                        REACTION = context.getString(R.string.astonished_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                    holder.binding.reactions.tvDisappointedRelievedEmoji -> {
                        REACTION = context.getString(R.string.disappointed_relieved_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION

                    }
                    holder.binding.reactions.tvPrayingHandsEmoji -> {
                        REACTION = context.getString(R.string.praying_hands_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                }
                reactionMessage.reaction = REACTION
                reactionMessage.messageId = messageId

                Timber.d("react: ${reactionMessage.reaction}, ${reactionMessage.messageId}")

                if (REACTION.isNotEmpty()) {
                    addReaction.invoke(reactionMessage)
                }

                holder.binding.cvReactions.visibility = View.GONE
                holder.binding.cvMessageOptions.visibility = View.GONE
                holder.binding.cvReactedEmoji.visibility = View.VISIBLE
            }
        }
    }

    // TODO - same listeners method for both holders
    private fun listeners(holder: SentMessageHolder, messageId: Int) {
        holder.binding.reactions.clEmoji.children.forEach { child ->
            child.setOnClickListener {
                when (child) {
                    holder.binding.reactions.tvThumbsUpEmoji -> {
                        REACTION = context.getString(R.string.thumbs_up_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                    holder.binding.reactions.tvHeartEmoji -> {
                        REACTION = context.getString(R.string.heart_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                    holder.binding.reactions.tvCryingEmoji -> {
                        REACTION = context.getString(R.string.crying_face_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                    holder.binding.reactions.tvAstonishedEmoji -> {
                        REACTION = context.getString(R.string.astonished_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                    holder.binding.reactions.tvDisappointedRelievedEmoji -> {
                        REACTION = context.getString(R.string.disappointed_relieved_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION

                    }
                    holder.binding.reactions.tvPrayingHandsEmoji -> {
                        REACTION = context.getString(R.string.praying_hands_emoji)
                        holder.binding.tvReactedEmoji.text = REACTION
                    }
                }
                reactionMessage.reaction = REACTION
                reactionMessage.messageId = messageId

                Timber.d("react: ${reactionMessage.reaction}, ${reactionMessage.messageId}")

                if (REACTION.isNotEmpty()) {
                    addReaction.invoke(reactionMessage)
                }

                holder.binding.cvReactions.visibility = View.GONE
                holder.binding.cvMessageOptions.visibility = View.GONE
                holder.binding.cvReactedEmoji.visibility = View.VISIBLE
            }
        }
    }


    private fun addFiles(message: Message, ivFileType: ImageView) {
        when (message.body?.file?.fileName?.substringAfterLast(".")) {
            Const.FileExtensions.PDF -> ivFileType.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_baseline_picture_as_pdf_24,
                    null
                )
            )
            Const.FileExtensions.ZIP, Const.FileExtensions.RAR -> ivFileType.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_baseline_folder_zip_24,
                    null
                )
            )
            Const.FileExtensions.MP3, Const.FileExtensions.WAW -> ivFileType.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_baseline_audio_file_24,
                    null
                )
            )
            else -> ivFileType.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_baseline_insert_drive_file_24,
                    null
                )
            )
        }
    }

    private class MessageAndRecordsDiffCallback : DiffUtil.ItemCallback<MessageAndRecords>() {

        override fun areItemsTheSame(
            oldItem: MessageAndRecords,
            newItem: MessageAndRecords
        ): Boolean {
            return oldItem.message == newItem.message
        }

        override fun areContentsTheSame(
            oldItem: MessageAndRecords,
            newItem: MessageAndRecords
        ): Boolean {
            return oldItem == newItem
        }
    }

    private fun showDateHeader(
        position: Int,
        date: Int,
        view: TextView,
        message: Message
    ) {
        if (position >= 0 && currentList.size - 1 > position) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = getItem(position + 1).message.createdAt!!
            val previousDate = calendar.get(Calendar.DAY_OF_MONTH)

            if (date != previousDate) {
                view.visibility = View.VISIBLE
            } else view.visibility = View.GONE

            view.text = message.createdAt?.let {
                DateUtils.getRelativeTimeSpanString(
                    it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS
                )
            }
        } else {
            view.visibility = View.VISIBLE
            view.text =
                message.createdAt?.let {
                    getRelativeTimeSpan(it)
                }
        }
    }
}
