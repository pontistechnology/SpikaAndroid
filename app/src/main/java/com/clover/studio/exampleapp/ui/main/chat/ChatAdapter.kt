package com.clover.studio.exampleapp.ui.main.chat

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.clover.studio.exampleapp.ChatAdapterHelper
import com.clover.studio.exampleapp.ChatAdapterHelper.addFiles
import com.clover.studio.exampleapp.ChatAdapterHelper.setViewsVisibility
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.MessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.databinding.ItemMessageMeBinding
import com.clover.studio.exampleapp.databinding.ItemMessageOtherBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.Tools.getRelativeTimeSpan
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


private const val VIEW_TYPE_MESSAGE_SENT = 1
private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
private var oldPosition = -1
private var firstPlay = true


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

            // View holder for my messages
            // TODO can two view holders use same method for binding if all views are the same?
            if (holder.itemViewType == VIEW_TYPE_MESSAGE_SENT) {
                holder as SentMessageHolder
                holder.binding.clContainer.setBackgroundResource(R.drawable.bg_message_user)
                holder.binding.tvTime.visibility = View.GONE
                when (it.message.type) {
                    Const.JsonFields.TEXT_TYPE -> {
                        setViewsVisibility(holder.binding.tvMessage, holder)
                        bindText(holder, holder.binding.tvMessage, it)
                        bindContainerListener(
                            holder.binding.tvTime,
                            holder.binding.clContainer,
                            calendar
                        )
                    }
                    Const.JsonFields.IMAGE_TYPE -> {
                        setViewsVisibility(holder.binding.cvImage, holder)
                        bindImage(
                            it,
                            holder.binding.ivChatImage,
                            holder.binding.clProgressScreen,
                            holder.binding.progressBar,
                            holder.binding.clContainer,
                        )
                    }
                    Const.JsonFields.FILE_TYPE -> {
                        setViewsVisibility(holder.binding.clFileMessage, holder)

                        if (it.message.body!!.file?.id == Const.JsonFields.TEMPORARY_FILE_ID) {
                            holder.binding.ivDownloadFile.visibility = View.GONE
                            holder.binding.ivCancelFile.visibility = View.VISIBLE
                            holder.binding.pbFile.visibility = View.VISIBLE
                            holder.binding.ivCancelFile.setOnClickListener { _ ->
                                onMessageInteraction(Const.UserActions.DOWNLOAD_CANCEL, it)
                            }
                            holder.binding.pbFile.secondaryProgress = it.message.uploadProgress
                        } else {
                            holder.binding.ivCancelFile.visibility = View.GONE
                            holder.binding.pbFile.visibility = View.GONE

                            bindFile(
                                it,
                                holder.binding.tvFileTitle,
                                holder.binding.tvFileSize,
                                holder.binding.ivDownloadFile
                            )
                            addFiles(
                                context,
                                it.message,
                                holder.binding.ivFileType
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

                        if (it.message.body?.file?.id == Const.JsonFields.TEMPORARY_FILE_ID) {
                            holder.binding.pbAudio.visibility = View.VISIBLE
                            holder.binding.ivPlayAudio.visibility = View.GONE
                            holder.binding.ivCancelAudio.visibility = View.VISIBLE
                            holder.binding.ivCancelAudio.setOnClickListener { _ ->
                                onMessageInteraction(Const.UserActions.DOWNLOAD_CANCEL, it)
                            }
                            Timber.d("Updating progress: ${it.message.uploadProgress}")
                            holder.binding.pbAudio.secondaryProgress = it.message.uploadProgress
                        } else {
                            holder.binding.pbAudio.visibility = View.GONE
                            holder.binding.ivCancelAudio.visibility = View.GONE
                            bindAudio(
                                holder,
                                it,
                                holder.binding.ivPlayAudio,
                                holder.binding.sbAudio,
                                holder.binding.tvAudioDuration
                            )
                        }
                    }
                    else -> {
                        setViewsVisibility(holder.binding.tvMessage, holder)
                    }
                }

                // Reply section
                ChatAdapterHelper.bindReply(
                    context,
                    users,
                    it,
                    holder.binding.ivReplyImage,
                    holder.binding.tvReplyMedia,
                    holder.binding.tvMessageReply,
                    holder.binding.clReplyMessage,
                    holder.binding.clContainer,
                    holder.binding.tvUsername
                )

                // Find replied message
                holder.binding.clReplyMessage.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.MESSAGE_REPLY, it)
                }

                // Show/hide message edited layout. If createdAt field doesn't correspond to the
                // modifiedAt field, we can conclude that the message was edited.
                if (it.message.deleted == false && it.message.createdAt != it.message.modifiedAt) {
                    holder.binding.clMessageEdited.visibility = View.VISIBLE
                } else {
                    holder.binding.clMessageEdited.visibility = View.GONE
                }

                /* Reactions section: */
                // Get reactions from database:
                ChatAdapterHelper.bindReactions(
                    it,
                    holder.binding.tvReactedEmoji,
                    holder.binding.cvReactedEmoji
                )

                holder.binding.cvReactedEmoji.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.SHOW_MESSAGE_REACTIONS, it)
                }

                // Send new reaction:
                sendReaction(it, holder.binding.clContainer, holder.absoluteAdapterPosition)

                showDateHeader(position, date, holder.binding.tvSectionHeader, it.message)
                ChatAdapterHelper.showMessageStatus(context, it, holder.binding.ivMessageStatus)

            /**
             * View holder for messages from other users
             */
            } else {
                holder as ReceivedMessageHolder

                holder.binding.clContainer.setBackgroundResource(R.drawable.bg_message_received)
                holder.binding.tvTime.visibility = View.GONE
                when (it.message.type) {
                    Const.JsonFields.TEXT_TYPE -> {
                        setViewsVisibility(holder.binding.tvMessage, holder)

                        bindText(holder, holder.binding.tvMessage, it)
                        bindContainerListener(
                            holder.binding.tvTime,
                            holder.binding.clContainer,
                            calendar
                        )
                    }
                    Const.JsonFields.IMAGE_TYPE -> {
                        setViewsVisibility(holder.binding.cvImage, holder)

                        bindImage(
                            it,
                            holder.binding.ivChatImage,
                            null,
                            null,
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
                        setViewsVisibility(holder.binding.clFileMessage, holder)
                        bindFile(
                            it,
                            holder.binding.tvFileTitle,
                            holder.binding.tvFileSize,
                            holder.binding.ivDownloadFile
                        )
                        addFiles(context, it.message, holder.binding.ivFileType)

                        holder.binding.ivDownloadFile.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                onMessageInteraction.invoke(
                                    Const.UserActions.DOWNLOAD_FILE,
                                    it
                                )
                            }
                            true
                        }
                    }
                    Const.JsonFields.AUDIO_TYPE -> {
                        setViewsVisibility(holder.binding.cvAudio, holder)
                        bindAudio(
                            holder,
                            it,
                            holder.binding.ivPlayAudio,
                            holder.binding.sbAudio,
                            holder.binding.tvAudioDuration
                        )
                    }
                    else -> {
                        setViewsVisibility(holder.binding.tvMessage, holder)
                    }
                }

                // Reply section
                ChatAdapterHelper.bindReply(
                    context,
                    users,
                    it,
                    holder.binding.ivReplyImage,
                    holder.binding.tvReplyMedia,
                    holder.binding.tvMessageReply,
                    holder.binding.clReplyMessage,
                    holder.binding.clContainer,
                    holder.binding.tvUsername
                )

                // Find replied message
                holder.binding.clReplyMessage.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.MESSAGE_REPLY, it)
                }

                // Show/hide message edited layout. If createdAt field doesn't correspond to the
                // modifiedAt field, we can conclude that the message was edited.
                if (it.message.deleted == false && it.message.createdAt != it.message.modifiedAt) {
                    holder.binding.clMessageEdited.visibility = View.VISIBLE
                } else {
                    holder.binding.clMessageEdited.visibility = View.GONE
                }

                if (roomType != Const.JsonFields.PRIVATE) {
                    for (roomUser in users) {
                        if (it.message.fromUserId == roomUser.id) {
                            holder.binding.tvUsername.text = roomUser.displayName
                            Glide.with(context)
                                .load(roomUser.avatarFileId?.let { fileId ->
                                    Tools.getFilePathUrl(
                                        fileId
                                    )
                                })
                                .placeholder(
                                    AppCompatResources.getDrawable(
                                        context,
                                        R.drawable.img_user_placeholder
                                    )
                                )
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(holder.binding.ivUserImage)
                            break
                        }
                    }
                    holder.binding.ivUserImage.visibility = View.VISIBLE
                    holder.binding.tvUsername.visibility = View.VISIBLE
                } else {
                    holder.binding.ivUserImage.visibility = View.GONE
                    holder.binding.tvUsername.visibility = View.GONE
                }

                /* Reactions section: */
                // Get reactions from database
                ChatAdapterHelper.bindReactions(
                    it,
                    holder.binding.tvReactedEmoji,
                    holder.binding.cvReactedEmoji
                )

                // Send new reaction:
                sendReaction(it, holder.binding.clContainer, holder.absoluteAdapterPosition)

                holder.binding.cvReactedEmoji.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.SHOW_MESSAGE_REACTIONS, it)
                }

                showDateHeader(position, date, holder.binding.tvSectionHeader, it.message)

                // TODO - show avatar only on last message and name on first message
                if (roomType != Const.JsonFields.PRIVATE) {
                    if (position > 0) {
                        try {
                            val nextItem = getItem(position + 1).message.fromUserId
                            val previousItem = getItem(position - 1).message.fromUserId

                            val currentItem = it.message.fromUserId
                            //Timber.d("Items : $nextItem, $currentItem ${nextItem == currentItem}")

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
            }
        }
    }

    private fun sendReaction(chatMessage: MessageAndRecords?, clContainer: ConstraintLayout, position: Int) {
        clContainer.setOnLongClickListener {
            chatMessage!!.message.senderMessage = true
            chatMessage.message.messagePosition = position
            onMessageInteraction.invoke(Const.UserActions.MESSAGE_ACTION, chatMessage)
            true
        }
    }

    private fun bindText(
        holder: ViewHolder,
        tvMessage: TextView,
        chatMessage: MessageAndRecords
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
            tvMessage.background =
                AppCompatResources.getDrawable(context, R.drawable.bg_message_user)
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
                chatMessage.message.senderMessage = true
                chatMessage.message.messagePosition = holder.absoluteAdapterPosition
                onMessageInteraction.invoke(Const.UserActions.MESSAGE_ACTION, chatMessage)
            }
            true
        }
    }

    private fun bindImage(
        chatMessage: MessageAndRecords,
        ivChatImage: ImageView,
        clProgressScreen: ConstraintLayout?,
        progressBar: ProgressBar?,
        clContainer: ConstraintLayout
    ) {

        val imagePath = chatMessage.message.body?.thumb?.id?.let { imagePath ->
            Tools.getFilePathUrl(
                imagePath
            )
        }

        // If sender
        if (chatMessage.message.body?.file?.uri != null) {
            if (clProgressScreen != null && progressBar != null) {
                clProgressScreen.visibility = View.VISIBLE
                Glide.with(context)
                    .load(imagePath)
                    .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                    .placeholder(R.drawable.img_image_placeholder)
                    .dontTransform()
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivChatImage)

                // Update the progress bar of the media item currently being uploaded
                progressBar.secondaryProgress = chatMessage.message.uploadProgress
            }
        } else {
            if (clProgressScreen != null) {
                clProgressScreen.visibility = View.GONE
            }

            Glide.with(context)
                .load(imagePath)
                .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                .placeholder(R.drawable.img_image_placeholder)
                .dontTransform()
                .dontAnimate()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivChatImage)
        }

        clContainer.setOnClickListener {
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

        exoPlayer.addListener(object : Player.Listener {
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
        })

        // Seek through audio
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
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
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

        Glide.with(context)
            .load(videoThumb)
            .priority(Priority.HIGH)
            .dontTransform()
            .dontAnimate()
            .placeholder(R.drawable.img_camera_black)
            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(ivVideoThumbnail)

        clVideos.visibility = View.VISIBLE
        ivPlayButton.setImageResource(R.drawable.img_play)

        ivPlayButton.setOnClickListener {
            onMessageInteraction(Const.UserActions.NAVIGATE_TO_MEDIA_FRAGMENT, chatMessage)
        }
    }

    private fun bindContainerListener(
        tvTime: TextView,
        clContainer: ConstraintLayout,
        calendar: Calendar
    ) {
        clContainer.setOnClickListener {
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
