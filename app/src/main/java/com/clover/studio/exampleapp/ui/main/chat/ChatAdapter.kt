package com.clover.studio.exampleapp.ui.main.chat

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.clover.studio.exampleapp.data.models.*
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
/*private var reactionMessage: ReactionMessage =
    ReactionMessage(
        "", 0, /*0, false,
        ReactionActive(
            thumbsUp = false,
            heart = false,
            prayingHandsEmoji = false,
            astonishedEmoji = false,
            relievedEmoji = false,
            cryingFaceEmoji = false
        ),
        0,*/
    )*/

class ChatAdapter(
    private val context: Context,
    private val myUserId: Int,
    private val users: List<User>,
    private val onMessageInteraction: ((event: String, message: Message) -> Unit)
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

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position).let {

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
                            .placeholder(R.drawable.img_image_placeholder)
                            .dontTransform()
                            .dontAnimate()
                            .into(holder.binding.ivChatImage)

                        holder.binding.clContainer.setOnClickListener { view ->
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
                        val sizeText =
                            Tools.calculateFileSize(it.message.body?.file?.size!!)
                                .toString()
                        holder.binding.tvFileSize.text = sizeText
                        addFiles(it.message, holder.binding.ivFileType)

                        val message = it.message
                        holder.binding.clFileMessage.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                onMessageInteraction.invoke(
                                    Const.UserActions.DOWNLOAD_FILE,
                                    message
                                )
                            }
                            true
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
                            .placeholder(R.drawable.img_camera_black)
                            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                            .into(holder.binding.ivVideoThumbnail)

                        holder.binding.clVideos.visibility = View.VISIBLE
                        holder.binding.ivPlayButton.setImageResource(R.drawable.img_play)

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

                // Show/hide message edited layout. If createdAt field doesn't correspond to the
                // modifiedAt field, we can conclude that the message was edited.
                if (it.message.deleted == false && it.message.createdAt != it.message.modifiedAt) {
                    holder.binding.clMessageEdited.visibility = View.VISIBLE
                } else {
                    holder.binding.clMessageEdited.visibility = View.GONE
                }

                /* Reactions section: */
                // All commented lines of code in the reactions section refer to removing reactions

                // Listener - remove reaction layouts
                holder.binding.clMessage.setOnTouchListener { _, _ ->
                    if (holder.binding.cvReactions.visibility == View.VISIBLE) {
                        holder.binding.cvReactions.visibility = View.GONE
                        holder.binding.cvMessageOptions.visibility = View.GONE
                    }
                    return@setOnTouchListener true
                }

                // Get reactions from database:
                // var reactionId = 0
                val reactions = Reactions(0, 0, 0, 0, 0, 0)
                val reactionText = getDatabaseReaction(it, reactions)

                // Show reactions if there are any in the database
                if (reactionText.isNotEmpty()) {
                    holder.binding.tvReactedEmoji.text = getGroupReactions(reactions)
                    holder.binding.cvReactedEmoji.visibility = View.VISIBLE
                } else {
                    holder.binding.cvReactedEmoji.visibility = View.GONE
                }

                // Send new reaction:
                holder.binding.clContainer.setOnLongClickListener { _ ->
                    holder.binding.cvReactions.visibility = View.VISIBLE
                    listeners(holder, it.message, it /*reactionId,*/)
                    holder.binding.cvMessageOptions.visibility = View.VISIBLE
                    true
                }

                // Delete message option clicked
                holder.binding.messageOptions.tvDelete.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.DELETE, it.message)
                    holder.binding.cvMessageOptions.visibility = View.GONE
                    holder.binding.cvReactions.visibility = View.GONE
                }

                // Edit message option clicked
                holder.binding.messageOptions.tvEdit.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.EDIT, it.message)
                    holder.binding.cvMessageOptions.visibility = View.GONE
                    holder.binding.cvReactions.visibility = View.GONE
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
                            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                            .placeholder(R.drawable.img_image_placeholder)
                            .dontTransform()
                            .dontAnimate()
                            .into(holder.binding.ivChatImage)

                        holder.binding.clContainer.setOnClickListener { view ->
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
                            .placeholder(R.drawable.img_camera_black)
                            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                            .into(holder.binding.ivVideoThumbnail)

                        holder.binding.clVideos.visibility = View.VISIBLE
                        holder.binding.ivPlayButton.setImageResource(R.drawable.img_play)

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
                        val sizeText =
                            Tools.calculateFileSize(it.message.body?.file?.size!!)
                                .toString()
                        holder.binding.tvFileSize.text = sizeText

                        addFiles(it.message, holder.binding.ivFileType)

                        val message = it.message
                        holder.binding.clFileMessage.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                onMessageInteraction.invoke(
                                    Const.UserActions.DOWNLOAD_FILE,
                                    message
                                )
                            }
                            true
                        }
                    }
                    else -> {
                        holder.binding.tvMessage.visibility = View.VISIBLE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                    }
                }

                // Show/hide message edited layout. If createdAt field doesn't correspond to the
                // modifiedAt field, we can conclude that the message was edited.
                if (it.message.deleted == false && it.message.createdAt != it.message.modifiedAt) {
                    holder.binding.clMessageEdited.visibility = View.VISIBLE
                } else {
                    holder.binding.clMessageEdited.visibility = View.GONE
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

                /* Reactions section: */
                // Listener - remove reaction layouts
                holder.binding.clMessageOther.setOnTouchListener { _, _ ->
                    if (holder.binding.cvReactions.visibility == View.VISIBLE) {
                        holder.binding.cvReactions.visibility = View.GONE
                        holder.binding.cvMessageOptions.visibility = View.GONE
                    }
                    return@setOnTouchListener true
                }

                // Get reactions from database
                val reactions = Reactions(0, 0, 0, 0, 0, 0)
                val reactionText = getDatabaseReaction(it, reactions)

                if (reactionText.isNotEmpty()) {
                    holder.binding.tvReactedEmoji.text = getGroupReactions(reactions)
                    holder.binding.cvReactedEmoji.visibility = View.VISIBLE
                } else {
                    holder.binding.cvReactedEmoji.visibility = View.GONE
                }

                // Send new reaction:
                holder.binding.clContainer.setOnLongClickListener { _ ->
                    holder.binding.cvReactions.visibility = View.VISIBLE
                    listeners(holder, it.message, /*reactionId*/ it)
                    true
                }

                showDateHeader(position, date, holder.binding.tvSectionHeader, it.message)

                // TODO - show avatar only on last message and name on first message
                if (position > 0) {
                    try {
                        val nextItem = getItem(position + 1).message.fromUserId
                        val previousItem = getItem(position - 1).message.fromUserId

                        val currentItem = it.message.fromUserId
                        //Timber.d("Items : $nextItem, $currentItem ${nextItem == currentItem}")

                        if (previousItem == currentItem) {
                            holder.binding.cvUserAvatar.visibility = View.INVISIBLE
                        } else {
                            holder.binding.cvUserAvatar.visibility = View.VISIBLE
                        }

                        if (nextItem == currentItem) {
                            holder.binding.tvUsername.visibility = View.GONE
                        } else {
                            holder.binding.tvUsername.visibility = View.VISIBLE
                        }
                    } catch (ex: IndexOutOfBoundsException) {
                        Tools.checkError(ex)
                        holder.binding.tvUsername.visibility = View.VISIBLE
                        holder.binding.cvUserAvatar.visibility = View.VISIBLE
                    }
                } else {
                    holder.binding.tvUsername.visibility = View.VISIBLE
                    holder.binding.cvUserAvatar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun getDatabaseReaction(
        messageAndRecords: MessageAndRecords,
        reactions: Reactions
    ): String {
        val filteredList = messageAndRecords.records?.filter { it.reaction != null }
        val sortedList = filteredList?.sortedByDescending { it.createdAt }
        val reactionList = sortedList?.distinctBy { it.userId }

        try {
            var reaction: String
            if (reactionList != null) {
                for (record in reactionList) {
                    reaction = record.reaction.toString()
                    getAllReactions(reaction, reactions)
                    // reactionId = record.id
                }
            }
        } catch (e: Exception) {
            Timber.d(e.toString())
        }
        return getGroupReactions(reactions)
    }

    // Get number of reactions for each one
    private fun getAllReactions(reaction: String, reactions: Reactions) {
        when (reaction) {
            context.getString(R.string.praying_hands_emoji) -> {
                reactions.prayingHandsEmoji++
            }
            context.getString(R.string.thumbs_up_emoji) -> {
                reactions.thumbsUp++
            }
            context.getString(R.string.heart_emoji) -> {
                reactions.heart++
            }
            context.getString(R.string.astonished_emoji) -> {
                reactions.astonishedEmoji++
            }
            context.getString(R.string.disappointed_relieved_emoji) -> {
                reactions.relievedEmoji++
            }
            context.getString(R.string.crying_face_emoji) -> {
                reactions.cryingFaceEmoji++
            }
            else -> {
                Timber.d("Not found")
            }
        }
    }

    // Get reaction and number of that reaction
    private fun getGroupReactions(reactions: Reactions): String {
        var reactionText = ""
        if (reactions.thumbsUp != 0) {
            reactionText += context.getString(R.string.thumbs_up_emoji) + " "
            if (reactions.thumbsUp > 1) {
                reactionText += reactions.thumbsUp.toString() + " "
            }
        }
        if (reactions.heart != 0) {
            reactionText += context.getString(R.string.heart_emoji) + " "
            if (reactions.heart > 1) {
                reactionText += reactions.heart.toString() + " "
            }
        }

        if (reactions.astonishedEmoji != 0) {
            reactionText += context.getString(R.string.astonished_emoji) + " "
            if (reactions.astonishedEmoji > 1) {
                reactionText += reactions.astonishedEmoji.toString() + " "
            }
        }

        if (reactions.prayingHandsEmoji != 0) {
            reactionText += context.getString(R.string.praying_hands_emoji) + " "
            if (reactions.prayingHandsEmoji > 1) {
                reactionText += reactions.prayingHandsEmoji.toString() + " "
            }
        }

        if (reactions.cryingFaceEmoji != 0) {
            reactionText += context.getString(R.string.crying_face_emoji) + " "
            if (reactions.cryingFaceEmoji > 1) {
                reactionText += reactions.cryingFaceEmoji.toString() + " "
            }
        }

        if (reactions.relievedEmoji != 0) {
            reactionText += context.getString(R.string.disappointed_relieved_emoji) + " "
            if (reactions.relievedEmoji > 1) {
                reactionText += reactions.relievedEmoji.toString() + " "
            }
        }
        return reactionText
    }

    // TODO - same listeners method for both holders
    private fun listeners(
        holder: ReceivedMessageHolder,
        message: Message,
        /*reactionId: Int,*/
        messageAndRecords: MessageAndRecords,
    ) {
        holder.binding.reactions.clEmoji.children.forEach { child ->
            child.setOnClickListener {
                when (child) {
                    holder.binding.reactions.tvThumbsUpEmoji -> {
                        /*
                        For removing reactions
                        if (reactionMessage.activeReaction!!.thumbsUp) {
                            // Active reaction is already thumbs up -> remove it
                            Timber.d("reactionId3: $reactionId")
                            reactionMessage.clicked = true
                            reactionMessage.activeReaction!!.thumbsUp = false
                        } else {
                            reactionMessage.activeReaction!!.thumbsUp = true*/
                        REACTION = context.getString(R.string.thumbs_up_emoji)
                    }
                    holder.binding.reactions.tvHeartEmoji -> {
                        REACTION = context.getString(R.string.heart_emoji)

                    }
                    holder.binding.reactions.tvCryingEmoji -> {
                        REACTION = context.getString(R.string.crying_face_emoji)

                    }
                    holder.binding.reactions.tvAstonishedEmoji -> {
                        REACTION = context.getString(R.string.astonished_emoji)

                    }
                    holder.binding.reactions.tvDisappointedRelievedEmoji -> {
                        REACTION = context.getString(R.string.disappointed_relieved_emoji)

                    }
                    holder.binding.reactions.tvPrayingHandsEmoji -> {
                        REACTION = context.getString(R.string.praying_hands_emoji)
                    }
                }

                //reactionMessage.reactionId = reactionId
                //reactionMessage.userId = myUserId

                if (REACTION.isNotEmpty()) {
                    message.reaction = REACTION
                    onMessageInteraction.invoke(
                        Const.UserActions.ADD_REACTION,
                        message
                    )
                    val reactions = Reactions(0, 0, 0, 0, 0, 0)
                    holder.binding.tvReactedEmoji.text =
                        getDatabaseReaction(messageAndRecords, reactions)
                    holder.binding.cvReactedEmoji.visibility = View.VISIBLE

                    /*if (reactionMessage.clicked) {
                        //holder.binding.cvReactedEmoji.visibility = View.GONE
                        reactionMessage.clicked = false
                    } else {
                        holder.binding.cvReactedEmoji.visibility = View.VISIBLE
                    }*/
                } else {
                    holder.binding.cvReactedEmoji.visibility = View.GONE
                }
                holder.binding.cvReactions.visibility = View.GONE
                holder.binding.cvMessageOptions.visibility = View.GONE
            }
        }
    }

    private fun listeners(
        holder: SentMessageHolder,
        message: Message,
        messageAndRecords: MessageAndRecords,
        /*reactionId: Int,*/
    ) {
        // TODO - remove my reaction for new one
        holder.binding.reactions.clEmoji.children.forEach { child ->
            child.setOnClickListener {
                when (child) {
                    holder.binding.reactions.tvThumbsUpEmoji -> {
                        /* For removing reactions
                        if (reactionMessage.activeReaction!!.thumbsUp) {
                            // Active reaction is already thumbs up -> remove it
                            reactionMessage.clicked = true
                            reactionMessage.activeReaction!!.thumbsUp = false
                        } else {
                            reactionMessage.activeReaction!!.thumbsUp = true*/
                        REACTION = context.getString(R.string.thumbs_up_emoji)
                    }
                    holder.binding.reactions.tvHeartEmoji -> {
                        REACTION = context.getString(R.string.heart_emoji)

                    }
                    holder.binding.reactions.tvCryingEmoji -> {
                        REACTION = context.getString(R.string.crying_face_emoji)

                    }
                    holder.binding.reactions.tvAstonishedEmoji -> {
                        REACTION = context.getString(R.string.astonished_emoji)

                    }
                    holder.binding.reactions.tvDisappointedRelievedEmoji -> {
                        REACTION = context.getString(R.string.disappointed_relieved_emoji)

                    }
                    holder.binding.reactions.tvPrayingHandsEmoji -> {
                        REACTION = context.getString(R.string.praying_hands_emoji)
                    }
                }

                //reactionMessage.reactionId = reactionId
                //reactionMessage.userId = myUserId

                if (REACTION.isNotEmpty()) {
                    message.reaction = REACTION
                    onMessageInteraction.invoke(
                        Const.UserActions.ADD_REACTION,
                        message
                    )
                    val reactions = Reactions(0, 0, 0, 0, 0, 0)
                    holder.binding.tvReactedEmoji.text =
                        getDatabaseReaction(messageAndRecords, reactions).trim()
                    holder.binding.cvReactedEmoji.visibility = View.VISIBLE

                    /*if (reactionMessage.clicked) {
                        //holder.binding.cvReactedEmoji.visibility = View.GONE
                        reactionMessage.clicked = false
                    } else {
                        holder.binding.cvReactedEmoji.visibility = View.VISIBLE
                    }*/
                } else {
                    holder.binding.cvReactedEmoji.visibility = View.GONE
                }
                holder.binding.cvReactions.visibility = View.GONE
                holder.binding.cvMessageOptions.visibility = View.GONE
            }
        }
    }


    private fun addFiles(message: Message, ivFileType: ImageView) {
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

    private class MessageAndRecordsDiffCallback : DiffUtil.ItemCallback<MessageAndRecords>() {

        override fun areItemsTheSame(
            oldItem: MessageAndRecords,
            newItem: MessageAndRecords
        ): Boolean {
            return oldItem.message.id == newItem.message.id
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