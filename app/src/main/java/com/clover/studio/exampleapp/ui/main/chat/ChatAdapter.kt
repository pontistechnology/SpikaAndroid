package com.clover.studio.exampleapp.ui.main.chat

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.MessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.databinding.ItemMessageMeBinding
import com.clover.studio.exampleapp.databinding.ItemMessageOtherBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.Tools.getRelativeTimeSpan
import com.clover.studio.exampleapp.utils.helpers.ChatAdapterHelper
import com.clover.studio.exampleapp.utils.helpers.ChatAdapterHelper.addFiles
import com.clover.studio.exampleapp.utils.helpers.ChatAdapterHelper.loadMedia
import com.clover.studio.exampleapp.utils.helpers.ChatAdapterHelper.setViewsVisibility
import com.clover.studio.exampleapp.utils.helpers.ChatAdapterHelper.showHideUserInformation
import java.text.SimpleDateFormat
import java.util.*

private const val VIEW_TYPE_MESSAGE_SENT = 1
private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
private const val MAX_UPLOAD_PROGRESS = 100
private var oldPosition = -1
private var firstPlay = true
private var playerListener: Player.Listener? = null

class ChatAdapter(
    private val context: Context,
    private val myUserId: Int,
    private val users: List<User>,
    private var exoPlayer: ExoPlayer,
    private var roomType: String?,
    private val onMessageInteraction: ((event: String, message: MessageAndRecords) -> Unit)
) :
    ListAdapter<MessageAndRecords, ViewHolder>(MessageAndRecordsDiffCallback()) {

    inner class SentMessageHolder(val binding: ItemMessageMeBinding) :
        ViewHolder(binding.root)

    inner class ReceivedMessageHolder(val binding: ItemMessageOtherBinding) :
        ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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

    private var handler = Handler(Looper.getMainLooper())

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position).let {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it.message.createdAt!!
            val date = calendar.get(Calendar.DAY_OF_MONTH)

            /** View holder for messages from sender */
            if (holder.itemViewType == VIEW_TYPE_MESSAGE_SENT) {
                holder as SentMessageHolder

                // The line below sets each adapter item to be unique (uses more memory)
                // holder.setIsRecyclable(false)

                if (playerListener != null) {
                    playerListener = null
                }

                holder.binding.clContainer.setBackgroundResource(R.drawable.bg_message_send)
                holder.binding.tvTime.visibility = View.GONE

                /** Message types: */
                when (it.message.type) {
                    Const.JsonFields.TEXT_TYPE -> {
                        setViewsVisibility(holder.binding.tvMessage, holder)
                        bindText(holder, holder.binding.tvMessage, it, true)
                        showMessageTime(
                            holder.binding.tvTime,
                            holder.binding.tvMessage,
                            calendar
                        )
                    }
                    Const.JsonFields.IMAGE_TYPE -> {
                        setViewsVisibility(holder.binding.ivChatImage, holder)

                        /** Uploading image: */
                        if (it.message.body?.file?.uri != null) {
                            holder.binding.clProgressScreen.visibility = View.VISIBLE
                            loadMedia(
                                context,
                                it.message.body.file?.uri!!,
                                holder.binding.ivChatImage,
                                AppCompatResources.getDrawable(
                                    context,
                                    R.drawable.img_image_placeholder
                                )
                            )
                            // Update the progress bar of the media item currently being uploaded
                            holder.binding.progressBar.secondaryProgress = it.message.uploadProgress
                            if (MAX_UPLOAD_PROGRESS == it.message.uploadProgress) {
                                holder.binding.clProgressScreen.visibility = View.GONE
                            }
                        } else {
                            holder.binding.clProgressScreen.visibility = View.GONE
                            bindImage(
                                it,
                                holder.binding.ivChatImage,
                                holder.binding.clContainer
                            )
                        }
                    }
                    Const.JsonFields.FILE_TYPE -> {
                        setViewsVisibility(holder.binding.fileLayout.clFileMessage, holder)
                        addFiles(
                            context,
                            holder.binding.fileLayout.ivFileType,
                            it.message.body?.file?.fileName?.substringAfterLast(".")!!
                        )
                        /** Uploading file: */
                        if (it.message.body.file?.id == Const.JsonFields.TEMPORARY_FILE_ID) {
                            holder.binding.fileLayout.ivDownloadFile.visibility = View.GONE
                            holder.binding.fileLayout.ivCancelFile.visibility = View.VISIBLE
                            holder.binding.fileLayout.pbFile.visibility = View.VISIBLE
                            holder.binding.fileLayout.tvFileTitle.text =
                                it!!.message.body?.file?.fileName
                            holder.binding.fileLayout.tvFileSize.text =
                                Tools.calculateFileSize(it.message.body.file?.size!!).toString()
                            holder.binding.fileLayout.pbFile.secondaryProgress =
                                it.message.uploadProgress
                            holder.binding.fileLayout.ivCancelFile.setOnClickListener { _ ->
                                onMessageInteraction(Const.UserActions.DOWNLOAD_CANCEL, it)
                            }
                        } else {
                            holder.binding.fileLayout.ivCancelFile.visibility = View.GONE
                            holder.binding.fileLayout.pbFile.visibility = View.GONE
                            holder.binding.fileLayout.clFileMessage.setBackgroundResource(R.drawable.bg_message_send)
                            bindFile(
                                it,
                                holder.binding.fileLayout.tvFileTitle,
                                holder.binding.fileLayout.tvFileSize,
                                holder.binding.fileLayout.ivDownloadFile
                            )
                        }
                    }
                    Const.JsonFields.VIDEO_TYPE -> {
                        setViewsVisibility(holder.binding.clVideos, holder)
                        bindVideo(
                            it,
                            holder.binding.ivVideoThumbnail,
                            holder.binding.clVideos,
                            holder.binding.ivPlayButton
                        )
                    }
                    Const.JsonFields.AUDIO_TYPE -> {
                        setViewsVisibility(holder.binding.cvAudio, holder)

                        /** Uploading audio: */
                        if (it.message.body?.file?.id == Const.JsonFields.TEMPORARY_FILE_ID) {
                            holder.binding.audioLayout.pbAudio.visibility = View.VISIBLE
                            holder.binding.audioLayout.ivPlayAudio.visibility = View.GONE
                            holder.binding.audioLayout.ivCancelAudio.visibility = View.VISIBLE
                            holder.binding.audioLayout.ivCancelAudio.setOnClickListener { _ ->
                                onMessageInteraction(Const.UserActions.DOWNLOAD_CANCEL, it)
                            }
                            holder.binding.audioLayout.pbAudio.secondaryProgress =
                                it.message.uploadProgress
                        } else {
                            holder.binding.audioLayout.pbAudio.visibility = View.GONE
                            holder.binding.audioLayout.ivCancelAudio.visibility = View.GONE
                            bindAudio(
                                holder,
                                it,
                                holder.binding.audioLayout.ivPlayAudio,
                                holder.binding.audioLayout.sbAudio,
                                holder.binding.audioLayout.tvAudioDuration
                            )
                        }
                    }
                    else -> {
                        setViewsVisibility(holder.binding.tvMessage, holder)
                    }
                }

                /** Other: */

                /** Show message reply: */
                if (it.message.replyId != null && it.message.replyId != 0L && it.message.deleted == false) {
                    ChatAdapterHelper.bindReply(
                        context,
                        users,
                        it,
                        holder.binding.ivReplyImage,
                        holder.binding.tvReplyMedia,
                        holder.binding.tvMessageReply,
                        holder.binding.clReplyMessage,
                        holder.binding.clContainer,
                        holder.binding.tvUsername,
                        true,
                    )
                }

                /** Find replied message: */
                holder.binding.clReplyMessage.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.MESSAGE_REPLY, it)
                }

                /** Show edited layout: */
                if (it.message.deleted == false && it.message.createdAt != it.message.modifiedAt) {
                    holder.binding.clMessageEdited.visibility = View.VISIBLE
                } else {
                    holder.binding.clMessageEdited.visibility = View.GONE
                }

                /** Show reactions: */
                ChatAdapterHelper.bindReactions(
                    it,
                    holder.binding.tvReactedEmoji,
                    holder.binding.cvReactedEmoji
                )

                holder.binding.cvReactedEmoji.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.SHOW_MESSAGE_REACTIONS, it)
                }

                /** Send new reaction: */
                sendReaction(it, holder.binding.clContainer, holder.absoluteAdapterPosition)

                /** Show date header: */
                showDateHeader(position, date, holder.binding.tvSectionHeader, it.message)

                ChatAdapterHelper.showMessageStatus(context, it, holder.binding.ivMessageStatus)

            } else {
                /** View holder for messages from other users */
                holder as ReceivedMessageHolder

                if (playerListener != null) {
                    playerListener = null
                }

                // The line below sets each adapter item to be unique (uses more memory)
                // holder.setIsRecyclable(false)

                holder.binding.clContainer.setBackgroundResource(R.drawable.bg_message_received)
                holder.binding.tvTime.visibility = View.GONE

                /** Message types: */
                when (it.message.type) {
                    Const.JsonFields.TEXT_TYPE -> {
                        setViewsVisibility(holder.binding.tvMessage, holder)
                        bindText(holder, holder.binding.tvMessage, it, false)
                        holder.binding.clContainer.setBackgroundResource(R.drawable.bg_message_received)
                        showMessageTime(
                            holder.binding.tvTime,
                            holder.binding.tvMessage,
                            calendar
                        )
                    }
                    Const.JsonFields.IMAGE_TYPE -> {
                        setViewsVisibility(holder.binding.ivChatImage, holder)
                        bindImage(
                            it,
                            holder.binding.ivChatImage,
                            holder.binding.clContainer
                        )
                    }
                    Const.JsonFields.VIDEO_TYPE -> {
                        setViewsVisibility(holder.binding.clVideos, holder)
                        bindVideo(
                            it,
                            holder.binding.ivVideoThumbnail,
                            holder.binding.clVideos,
                            holder.binding.ivPlayButton
                        )

                    }
                    Const.JsonFields.FILE_TYPE -> {
                        setViewsVisibility(holder.binding.fileLayout.clFileMessage, holder)
                        holder.binding.fileLayout.clFileMessage.setBackgroundResource(R.drawable.bg_message_received)
                        bindFile(
                            it,
                            holder.binding.fileLayout.tvFileTitle,
                            holder.binding.fileLayout.tvFileSize,
                            holder.binding.fileLayout.ivDownloadFile
                        )
                        addFiles(
                            context,
                            holder.binding.fileLayout.ivFileType,
                            it.message.body?.file?.fileName?.substringAfterLast(".")!!
                        )
                    }
                    Const.JsonFields.AUDIO_TYPE -> {
                        setViewsVisibility(holder.binding.cvAudio, holder)
                        bindAudio(
                            holder,
                            it,
                            holder.binding.audioLayout.ivPlayAudio,
                            holder.binding.audioLayout.sbAudio,
                            holder.binding.audioLayout.tvAudioDuration
                        )
                    }
                    else -> {
                        setViewsVisibility(holder.binding.tvMessage, holder)
                    }
                }

                /** Other: */

                /** Show message reply: */
                if (it.message.replyId != null && it.message.replyId != 0L && it.message.deleted == false) {
                    ChatAdapterHelper.bindReply(
                        context,
                        users,
                        it,
                        holder.binding.ivReplyImage,
                        holder.binding.tvReplyMedia,
                        holder.binding.tvMessageReply,
                        holder.binding.clReplyMessage,
                        holder.binding.clContainer,
                        holder.binding.tvUsernameOther,
                        false,
                    )
                }

                /** Find replied message: */
                holder.binding.clReplyMessage.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.MESSAGE_REPLY, it)
                }

                /** Show edited layout: */
                if (it.message.deleted == false && it.message.createdAt != it.message.modifiedAt) {
                    holder.binding.clMessageEdited.visibility = View.VISIBLE
                } else {
                    holder.binding.clMessageEdited.visibility = View.GONE
                }

                /** Show user names and avatars in group chat */
                if (Const.JsonFields.PRIVATE == roomType) {
                    holder.binding.ivUserImage.visibility = View.GONE
                    holder.binding.tvUsername.visibility = View.GONE
                } else {
                    for (roomUser in users) {
                        if (it.message.fromUserId == roomUser.id) {
                            holder.binding.tvUsername.text = roomUser.displayName
                            val userPath = roomUser.avatarFileId?.let { fileId ->
                                Tools.getFilePathUrl(fileId)
                            }
                            loadMedia(
                                context,
                                userPath!!,
                                holder.binding.ivUserImage,
                                AppCompatResources.getDrawable(
                                    context,
                                    R.drawable.img_user_placeholder
                                )
                            )
                        }
                    }
                    holder.binding.ivUserImage.visibility = View.VISIBLE
                    holder.binding.tvUsername.visibility = View.VISIBLE
                }

                /** Show reactions: */
                ChatAdapterHelper.bindReactions(
                    it,
                    holder.binding.tvReactedEmoji,
                    holder.binding.cvReactedEmoji
                )

                /** Send new reaction: */
                sendReaction(it, holder.binding.clContainer, holder.absoluteAdapterPosition)

                holder.binding.cvReactedEmoji.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.SHOW_MESSAGE_REACTIONS, it)
                }

                /** Show date header: */
                showDateHeader(position, date, holder.binding.tvSectionHeader, it.message)

                /** Show username and avatar only once in multiple consecutive messages */
                if (roomType != Const.JsonFields.PRIVATE) {
                    showHideUserInformation(position, holder, currentList)
                }
            }
        }

    }

    /** Methods that bind different types of messages: */
    private fun bindText(
        holder: ViewHolder,
        tvMessage: TextView,
        chatMessage: MessageAndRecords,
        sender: Boolean,
    ) {
        if (chatMessage.message.deleted == true || (chatMessage.message.body?.text == context.getString(
                R.string.deleted_message
            ) && (chatMessage.message.modifiedAt != chatMessage.message.createdAt))
        ) {
            tvMessage.text =
                context.getString(R.string.message_deleted_text)
            tvMessage.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            tvMessage.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.text_tertiary
                )
            )
            tvMessage.background = AppCompatResources.getDrawable(
                context,
                R.drawable.img_deleted_message
            )
        } else {
            tvMessage.text = chatMessage.message.body?.text
            if (sender) {
                tvMessage.background =
                    AppCompatResources.getDrawable(context, R.drawable.bg_message_send)
            } else {
                tvMessage.background =
                    AppCompatResources.getDrawable(context, R.drawable.bg_message_received)
            }
            tvMessage.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.text_primary
                )
            )
        }

        tvMessage.movementMethod = LinkMovementMethod.getInstance()
        tvMessage.setOnLongClickListener {
            if (!(chatMessage.message.deleted == true || (chatMessage.message.body?.text == context.getString(
                    R.string.deleted_message
                ) && (chatMessage.message.modifiedAt != chatMessage.message.createdAt)))
            ) {
                chatMessage.message.messagePosition = holder.absoluteAdapterPosition
                onMessageInteraction.invoke(Const.UserActions.MESSAGE_ACTION, chatMessage)
            }
            true
        }
    }

    private fun bindImage(
        chatMessage: MessageAndRecords,
        ivChatImage: ImageView,
        clContainer: ConstraintLayout
    ) {

        val imagePath = chatMessage.message.body?.thumb?.id?.let { imagePath ->
            Tools.getFilePathUrl(
                imagePath
            )
        }

        loadMedia(
            context,
            imagePath!!,
            ivChatImage,
            AppCompatResources.getDrawable(context, R.drawable.img_image_placeholder)
        )

        clContainer.setOnClickListener {
            onMessageInteraction(Const.UserActions.NAVIGATE_TO_MEDIA_FRAGMENT, chatMessage)
        }

        return
    }

    private fun bindVideo(
        chatMessage: MessageAndRecords,
        ivVideoThumbnail: ImageView,
        clVideos: ConstraintLayout,
        ivPlayButton: ImageView
    ) {
        val videoThumb = chatMessage.message.body?.thumb?.id?.let { videoThumb ->
            Tools.getFilePathUrl(
                videoThumb
            )
        }

        loadMedia(
            context,
            videoThumb!!,
            ivVideoThumbnail,
            AppCompatResources.getDrawable(context, R.drawable.img_camera_black)
        )

        clVideos.visibility = View.VISIBLE
        ivPlayButton.setImageResource(R.drawable.img_play)

        ivPlayButton.setOnClickListener {
            onMessageInteraction(Const.UserActions.NAVIGATE_TO_MEDIA_FRAGMENT, chatMessage)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindFile(
        it: MessageAndRecords?,
        tvFileTitle: TextView,
        tvFileSize: TextView,
        ivDownloadFile: ImageView
    ) {
        ivDownloadFile.visibility = View.VISIBLE
        tvFileTitle.text = it!!.message.body?.file?.fileName
        val sizeText =
            Tools.calculateFileSize(it.message.body?.file?.size!!)
                .toString()
        tvFileSize.text = sizeText

        ivDownloadFile.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                onMessageInteraction.invoke(
                    Const.UserActions.DOWNLOAD_FILE,
                    it
                )
            }
            true
        }
    }

    private fun bindAudio(
        holder: ViewHolder,
        chatMessage: MessageAndRecords?,
        ivPlayAudio: ImageView,
        sbAudio: SeekBar,
        tvAudioDuration: TextView
    ) {
        ivPlayAudio.visibility = View.VISIBLE
        val audioPath = chatMessage!!.message.body?.file?.id?.let { audioPath ->
            Tools.getFilePathUrl(
                audioPath
            )
        }

        val mediaItem: MediaItem = MediaItem.fromUri(Uri.parse(audioPath))
        exoPlayer.clearMediaItems()
        sbAudio.progress = 0

        val runnable = object : Runnable {
            override fun run() {
                sbAudio.progress =
                    exoPlayer.currentPosition.toInt()
                tvAudioDuration.text =
                    Tools.convertDurationMillis(exoPlayer.currentPosition)
                handler.postDelayed(this, 100)
            }
        }

        playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    sbAudio.max = exoPlayer.duration.toInt()
                }
                if (state == Player.STATE_ENDED) {
                    ivPlayAudio.visibility = View.VISIBLE
                    firstPlay = true
                    exoPlayer.pause()
                    exoPlayer.clearMediaItems()
                    handler.removeCallbacks(runnable)
                    tvAudioDuration.text =
                        context.getString(R.string.audio_duration)
                    ivPlayAudio.setImageResource(R.drawable.img_play_audio_button)
                }
            }
        }

        exoPlayer.addListener(playerListener!!)

        ivPlayAudio.setOnClickListener {
            if (!exoPlayer.isPlaying) {
                if (oldPosition != holder.absoluteAdapterPosition) {
                    firstPlay = true
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    tvAudioDuration.text =
                        context.getString(R.string.audio_duration)
                    handler.removeCallbacks(runnable)
                    notifyItemChanged(oldPosition)
                    oldPosition = holder.absoluteAdapterPosition
                }
                if (firstPlay) {
                    exoPlayer.prepare()
                    exoPlayer.setMediaItem(mediaItem)
                }
                exoPlayer.play()
                handler.postDelayed(runnable, 0)
                ivPlayAudio.setImageResource(R.drawable.img_pause_audio_button)
            } else {
                ivPlayAudio.setImageResource(R.drawable.img_play_audio_button)
                exoPlayer.pause()
                firstPlay = false
                handler.removeCallbacks(runnable)
            }
        }

        sbAudio.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    exoPlayer.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Ignore
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Ignore
            }
        })
    }

    /** A method that sends a reaction to a message */
    private fun sendReaction(
        chatMessage: MessageAndRecords?,
        clContainer: ConstraintLayout,
        position: Int
    ) {
        clContainer.setOnLongClickListener {
            chatMessage!!.message.messagePosition = position
            onMessageInteraction.invoke(Const.UserActions.MESSAGE_ACTION, chatMessage)
            true
        }
    }

    /** A method that shows the time of a message when it is tapped */
    private fun showMessageTime(
        tvTime: TextView,
        tvMessage: TextView,
        calendar: Calendar
    ) {
        tvMessage.setOnClickListener {
            if (tvTime.visibility == View.GONE) {
                tvTime.visibility = View.VISIBLE
                val simpleDateFormat =
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                val dateTime =
                    simpleDateFormat.format(calendar.timeInMillis).toString()
                tvTime.text = dateTime
            } else {
                tvTime.visibility = View.GONE
            }
        }
    }

    /** A method that displays a date header - eg Yesterday, Two days ago, Now*/
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

            val time = message.createdAt?.let {
                DateUtils.getRelativeTimeSpanString(
                    it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS
                )
            }

            if (time?.equals(context.getString(R.string.zero_minutes_ago)) == true) {
                view.text = context.getString(R.string.now)
            } else {
                view.text = time
            }
        } else {
            view.visibility = View.VISIBLE
            val time = message.createdAt?.let {
                getRelativeTimeSpan(it)
            }

            if (time?.equals(context.getString(R.string.zero_minutes_ago)) == true) {
                view.text = context.getString(R.string.now)
            } else {
                view.text = time
            }
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
}
