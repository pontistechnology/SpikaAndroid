package com.clover.studio.spikamessenger.ui.main.chat

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.MessageBody
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.databinding.AudioLayoutBinding
import com.clover.studio.spikamessenger.databinding.ItemMessageMeBinding
import com.clover.studio.spikamessenger.databinding.ItemMessageOtherBinding
import com.clover.studio.spikamessenger.databinding.ItemSystemMessageBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.Tools.getRelativeTimeSpan
import com.clover.studio.spikamessenger.utils.helpers.ChatAdapterHelper
import com.clover.studio.spikamessenger.utils.helpers.ChatAdapterHelper.setViewsVisibility
import com.clover.studio.spikamessenger.utils.helpers.ChatAdapterHelper.showHideUserInformation
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.vanniktech.emoji.EmojiTextView
import com.vanniktech.emoji.isOnlyEmojis
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

private const val VIEW_TYPE_MESSAGE_SENT = 1
private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
private const val VIEW_TYPE_SYSTEM_MESSAGE = 3
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

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class SentMessageHolder(val binding: ItemMessageMeBinding) :
        ViewHolder(binding.root)

    inner class ReceivedMessageHolder(val binding: ItemMessageOtherBinding) :
        ViewHolder(binding.root)

    inner class SystemMessageHolder(val binding: ItemSystemMessageBinding) :
        ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SYSTEM_MESSAGE -> {
                val binding = ItemSystemMessageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SystemMessageHolder(binding)
            }

            VIEW_TYPE_MESSAGE_SENT -> {
                val binding =
                    ItemMessageMeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SentMessageHolder(binding)
            }

            else -> {
                val binding = ItemMessageOtherBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageHolder(binding)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            Const.JsonFields.SYSTEM_TYPE == message.message.type -> VIEW_TYPE_SYSTEM_MESSAGE
            message.message.fromUserId == myUserId -> VIEW_TYPE_MESSAGE_SENT
            else -> VIEW_TYPE_MESSAGE_RECEIVED
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
            when (holder.itemViewType) {
                VIEW_TYPE_MESSAGE_SENT -> {
                    holder as SentMessageHolder

                    if (selectedPosition != 0 && selectedPosition == position) {
                        animateSelectedMessage(holder.itemView)
                        selectedPosition = 0
                    } else {
                        holder.itemView.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                android.R.color.transparent
                            )
                        )
                    }

                    if (playerListener != null) {
                        playerListener = null
                    }

                    // The line below sets each adapter item to be unique (uses more memory)
                    // holder.setIsRecyclable(false)
                    // System messages are displaying time so we don't need this interaction
                    it.message.type?.let { type ->
                        bindMessageTime(
                            tvTime = holder.binding.tvTime,
                            clContainer = holder.binding.clMessage,
                            calendar = calendar,
                            type = type
                        )
                    }

                    /** Message types: */
                    when (it.message.type) {
                        Const.JsonFields.TEXT_TYPE -> {
                            setViewsVisibility(holder.binding.tvMessage, holder)
                            bindText(
                                holder = holder,
                                tvMessage = holder.binding.tvMessage,
                                cvReactedEmoji = holder.binding.cvReactedEmoji,
                                chatMessage = it,
                                sender = true
                            )
                        }

                        Const.JsonFields.IMAGE_TYPE -> {
                            setViewsVisibility(holder.binding.cvImage, holder)
                            setImageLayout(
                                chatMessage = it,
                                container = holder.binding.flImageContainer,
                                sender = true
                            )
                        }

                        Const.JsonFields.VIDEO_TYPE -> {
                            if (it.message.id < 0) {
                                setViewsVisibility(holder.binding.cvImage, holder)
                                setImageLayout(
                                    chatMessage = it,
                                    container = holder.binding.flImageContainer,
                                    sender = true
                                )
                            } else {
                                setViewsVisibility(holder.binding.cvVideo, holder)
                                setVideoLayout(
                                    chatMessage = it,
                                    container = holder.binding.flVideoContainer,
                                    sender = true
                                )
                            }
                        }

                        Const.JsonFields.FILE_TYPE -> {
                            setViewsVisibility(holder.binding.cvFiles, holder)
                            setFileLayout(
                                chatMessage = it,
                                container = holder.binding.flFileContainer,
                                sender = true
                            )
                        }

                        Const.JsonFields.AUDIO_TYPE -> {
                            setViewsVisibility(holder.binding.cvAudio, holder)

                            /** Uploading audio: */
                            holder.binding.audioLayout.apply {
                                if (it.message.id < 0) {
                                    if (Resource.Status.LOADING.toString() == it.message.messageStatus) {
                                        ivPlayAudio.visibility = View.GONE
                                        pbAudio.apply {
                                            visibility = View.VISIBLE
                                            secondaryProgress = it.message.uploadProgress
                                        }
                                        ivCancelAudio.apply {
                                            visibility = View.VISIBLE
                                            setOnClickListener { _ ->
                                                onMessageInteraction(
                                                    Const.UserActions.DOWNLOAD_CANCEL,
                                                    it
                                                )
                                            }
                                        }
                                    } else {
                                        ivCancelAudio.visibility = View.GONE
                                        ivPlayAudio.visibility = View.GONE
                                        pbAudio.apply {
                                            visibility = View.GONE
                                            secondaryProgress = 0
                                        }
                                        ivUploadFailed.apply {
                                            visibility = View.VISIBLE
                                            setOnClickListener { _ ->
                                                onMessageInteraction(
                                                    Const.UserActions.RESEND_MESSAGE,
                                                    it
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    pbAudio.visibility = View.GONE
                                    ivCancelAudio.visibility = View.GONE
                                    ivUploadFailed.visibility = View.GONE
                                    bindAudio(
                                        holder = holder,
                                        chatMessage = it,
                                        audioLayoutBinding = holder.binding.audioLayout
                                    )
                                }
                            }
                        }

                        else -> {
                            setViewsVisibility(holder.binding.tvMessage, holder)
                        }
                    }

                    /** Other: */

                    /** Show message reply: */
                    if (it.message.replyId != null && it.message.replyId != 0L && it.message.deleted == false) {
                        setViewsVisibility(holder.binding.flReplyContainer, holder)
                        holder.binding.tvMessage.visibility = View.VISIBLE

                        setUpReplyLayout(
                            chatMessage = it,
                            parentContainer = holder.binding.clContainer,
                            replyContainer = holder.binding.flReplyContainer,
                            sender = true
                        )
                    }

                    /** Show edited layout: */
                    holder.binding.tvEdited.visibility =
                        if (it.message.deleted == false && it.message.createdAt != it.message.modifiedAt) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    holder.binding.tvForward.visibility = if (it.message.isForwarded &&
                        (it.message.deleted != null && !it.message.deleted)
                    ) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    /** Show reactions: */
                    holder.binding.cvReactedEmoji.visibility = View.GONE
                    if (it.message.deleted != null && !it.message.deleted) {
                        ChatAdapterHelper.bindReactions(
                            chatMessage = it,
                            tvReactedEmoji = holder.binding.tvReactedEmoji,
                            cvReactedEmoji = holder.binding.cvReactedEmoji
                        )
                    }

                    holder.binding.cvReactedEmoji.setOnClickListener { _ ->
                        onMessageInteraction.invoke(Const.UserActions.SHOW_MESSAGE_REACTIONS, it)
                    }

                    /** Send new reaction: */
                    sendReaction(it, holder.binding.clContainer, holder.absoluteAdapterPosition)

                    /** Show date header: */
                    showDateHeader(position, date, holder.binding.tvSectionHeader, it.message)

                    ChatAdapterHelper.showMessageStatus(it, holder.binding.ivMessageStatus)

                    addMargins(position, true, holder.binding.clMessage)

                }

                VIEW_TYPE_MESSAGE_RECEIVED -> {
                    /** View holder for messages from other users */
                    holder as ReceivedMessageHolder

                    if (selectedPosition != 0 && selectedPosition == position) {
                        animateSelectedMessage(holder.itemView)
                        selectedPosition = 0
                    } else {
                        holder.itemView.setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                android.R.color.transparent
                            )
                        )
                    }

                    if (playerListener != null) {
                        playerListener = null
                    }

                    // The line below sets each adapter item to be unique (uses more memory)
                    // holder.setIsRecyclable(false)
                    // System messages are displaying time so we don't need this interaction
                    it.message.type?.let { type ->
                        bindMessageTime(
                            tvTime = holder.binding.tvTime,
                            clContainer = holder.binding.clMessage,
                            calendar = calendar,
                            type = type
                        )
                    }

                    /** Message types: */
                    when (it.message.type) {
                        Const.JsonFields.TEXT_TYPE -> {
                            setViewsVisibility(holder.binding.tvMessage, holder)
                            bindText(
                                holder = holder,
                                tvMessage = holder.binding.tvMessage,
                                cvReactedEmoji = holder.binding.cvReactedEmoji,
                                chatMessage = it,
                                sender = false,
                            )
                        }

                        Const.JsonFields.IMAGE_TYPE -> {
                            setViewsVisibility(holder.binding.cvImage, holder)
                            setImageLayout(
                                chatMessage = it,
                                container = holder.binding.flImageContainer,
                                sender = false
                            )
                        }

                        Const.JsonFields.VIDEO_TYPE -> {
                            setViewsVisibility(holder.binding.cvVideo, holder)
                            setVideoLayout(
                                chatMessage = it,
                                container = holder.binding.flVideoContainer,
                                sender = false
                            )
                        }

                        Const.JsonFields.FILE_TYPE -> {
                            setViewsVisibility(holder.binding.cvFiles, holder)
                            setFileLayout(
                                chatMessage = it,
                                container = holder.binding.flFileContainer,
                                sender = false
                            )
                        }

                        Const.JsonFields.AUDIO_TYPE -> {
                            setViewsVisibility(holder.binding.cvAudio, holder)
                            bindAudio(
                                holder = holder,
                                chatMessage = it,
                                audioLayoutBinding = holder.binding.audioLayout
                            )
                        }

                        else -> {
                            setViewsVisibility(holder.binding.tvMessage, holder)
                        }
                    }

                    /** Other: */
                    if (it.message.replyId != null && it.message.replyId != 0L && it.message.deleted == false) {
                        setViewsVisibility(holder.binding.flReplyContainer, holder)
                        holder.binding.tvMessage.visibility = View.VISIBLE

                        setUpReplyLayout(
                            chatMessage = it,
                            parentContainer = holder.binding.clContainer,
                            replyContainer = holder.binding.flReplyContainer,
                            sender = false
                        )
                    }

                    holder.binding.tvForward.visibility = if (it.message.isForwarded) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    /** Show edited layout: */
                    holder.binding.tvEdited.visibility =
                        if (it.message.deleted == false && it.message.createdAt != it.message.modifiedAt) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    /** Show user names and avatars in group chat */
                    if (Const.JsonFields.PRIVATE == roomType) {
                        holder.binding.ivUserImage.visibility = View.GONE
                        holder.binding.tvUsername.visibility = View.GONE
                    } else {
                        val roomUser = users.find { user -> user.id == it.message.fromUserId }
                        if (roomUser != null) {
                            holder.binding.tvUsername.text =
                                if (roomUser.formattedDisplayName.length > 20) "${
                                    roomUser.formattedDisplayName.take(
                                        20
                                    )
                                }..." else roomUser.formattedDisplayName
                            val userPath = roomUser.avatarFileId?.let { fileId ->
                                Tools.getFilePathUrl(fileId)
                            }
                            Glide.with(context)
                                .load(userPath)
                                .dontTransform()
                                .placeholder(R.drawable.img_user_avatar)
                                .error(R.drawable.img_user_avatar)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(holder.binding.ivUserImage)

                            holder.binding.ivUserImage.visibility = View.VISIBLE
                            holder.binding.tvUsername.visibility = View.VISIBLE
                        } else {
                            // User probably doesn't exist in the room anymore
                            holder.binding.tvUsername.text =
                                context.getString(R.string.removed_group_user)

                            Glide.with(context)
                                .load(R.drawable.img_user_avatar)
                                .dontTransform()
                                .placeholder(R.drawable.img_user_avatar)
                                .error(R.drawable.img_user_avatar)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(holder.binding.ivUserImage)

                            holder.binding.ivUserImage.visibility = View.VISIBLE
                            holder.binding.tvUsername.visibility = View.VISIBLE
                        }
                    }

                    /** Show reactions: */
                    holder.binding.cvReactedEmoji.visibility = View.GONE
                    if (it.message.deleted != null && !it.message.deleted) {
                        ChatAdapterHelper.bindReactions(
                            chatMessage = it,
                            tvReactedEmoji = holder.binding.tvReactedEmoji,
                            cvReactedEmoji = holder.binding.cvReactedEmoji
                        )
                    }

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

                    addMargins(position, false, holder.binding.clMessage)
                }

                VIEW_TYPE_SYSTEM_MESSAGE -> {
                    holder as SystemMessageHolder
                    bindSystemMessage(holder.binding.tvSystemMessage, it)
                }

                else -> Timber.d("Error")
            }
        }

    }

    private fun setUpReplyLayout(chatMessage: MessageAndRecords, parentContainer: ConstraintLayout , replyContainer: FrameLayout, sender: Boolean) {
        val reply = ReplyLayout(context)
        reply.setReplyLayoutListener(object : ReplyLayout.ReplyLayoutListener {
            override fun replyLayoutClick() {
                onMessageInteraction.invoke(Const.UserActions.MESSAGE_REPLY, chatMessage)
            }
        })
        reply.bindReply(
            context = context,
            chatMessage = chatMessage,
            users = users,
            clContainer = parentContainer,
            sender = true,
            roomType = roomType
        )
        replyContainer.addView(reply)
    }

    private fun setFileLayout(
        chatMessage: MessageAndRecords,
        container: FrameLayout,
        sender: Boolean
    ) {
        val fileLayout = FileLayout(context)
        fileLayout.setFileLayoutListener(object :
            FileLayout.FileLayoutListener {
            override fun downloadFile() {
                onMessageInteraction.invoke(Const.UserActions.DOWNLOAD_FILE, chatMessage)
            }

            override fun resendFile() {
                onMessageInteraction(Const.UserActions.RESEND_MESSAGE, chatMessage)
            }

            override fun cancelFileUpload() {
                onMessageInteraction(Const.UserActions.DOWNLOAD_CANCEL, chatMessage)
            }
        })

        fileLayout.bindFile(chatMessage = chatMessage, sender = true)
        container.addView(fileLayout)
    }

    private fun setVideoLayout(
        chatMessage: MessageAndRecords,
        container: FrameLayout,
        sender: Boolean
    ) {
        val video = VideoLayout(context)
        video.setVideoLayoutListener(object : VideoLayout.VideoLayoutListener {
            override fun mediaNavigation() {
                onMessageInteraction(Const.UserActions.NAVIGATE_TO_MEDIA_FRAGMENT, chatMessage)
            }
        })
        video.bindVideo(chatMessage = chatMessage)
        container.addView(video)
    }

    private fun setImageLayout(
        chatMessage: MessageAndRecords,
        sender: Boolean,
        container: FrameLayout
    ) {
        val image = ImageLayout(context)
        image.setImageLayoutListener(object : ImageLayout.ImageLayoutListener {
            override fun imageNavigation() {
                onMessageInteraction(Const.UserActions.NAVIGATE_TO_MEDIA_FRAGMENT, chatMessage)
            }

            override fun imageResend() {
                onMessageInteraction(Const.UserActions.RESEND_MESSAGE, chatMessage)
            }

            override fun imageCancelUpload() {
                onMessageInteraction(Const.UserActions.DOWNLOAD_CANCEL, chatMessage)
            }

            override fun imageOptions() {
                onMessageInteraction(Const.UserActions.MESSAGE_ACTION, chatMessage)
            }
        })
        image.bindImage(chatMessage = chatMessage, sender = sender)
        container.addView(image)
    }

    private fun addMargins(position: Int, sender: Boolean, clMessage: ConstraintLayout) {
        val layoutParams = clMessage.layoutParams as ViewGroup.MarginLayoutParams

        if (position != currentList.size - 1) {
            val type = getItemViewType(position + 1)
            val margin =
                if (sender && type == VIEW_TYPE_MESSAGE_RECEIVED || !sender && type == VIEW_TYPE_MESSAGE_SENT) {
                    context.resources.getDimensionPixelSize(R.dimen.sixteen_dp_margin)
                } else {
                    context.resources.getDimensionPixelSize(R.dimen.four_dp_margin)
                }
            layoutParams.topMargin = margin
        }
        clMessage.layoutParams = layoutParams
    }

    private fun bindMessageTime(
        calendar: Calendar,
        tvTime: TextView,
        clContainer: ConstraintLayout,
        type: String
    ) {
        tvTime.visibility = View.GONE
        tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
            calendar.timeInMillis
        ).toString()

        if (Const.JsonFields.SYSTEM_TYPE != type) {
            clContainer.setOnClickListener {
                tvTime.visibility = if (tvTime.visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            }
        } else {
            clContainer.setOnClickListener(null)
        }
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyItemChanged(position)
    }

    /** A method that sets the color of the selected message to the selected_message color and
     *  then changes its alpha value to change from that color to transparent
     *  (alpha from 255 to 0)*/
    private fun animateSelectedMessage(itemView: View) {
        val alphaAnimator = ObjectAnimator.ofInt(255, 0)
        alphaAnimator.duration = 2000

        alphaAnimator.addUpdateListener { animator ->
            val alpha = animator.animatedValue as Int
            val backgroundDrawable =
                ColorDrawable(ContextCompat.getColor(context, R.color.selected_message))
            backgroundDrawable.alpha = alpha
            itemView.background = backgroundDrawable
        }

        alphaAnimator.start()
    }

    /** Methods that bind different types of messages: */
    private fun bindText(
        holder: ViewHolder,
        tvMessage: EmojiTextView,
        cvReactedEmoji: CardView,
        chatMessage: MessageAndRecords,
        sender: Boolean
    ) {
        val messageText = chatMessage.message.body?.text.toString()
        if (messageText.isOnlyEmojis()) {
            tvMessage.setEmojiSize(Tools.getEmojiSize(messageText))
        } else {
            tvMessage.background = AppCompatResources.getDrawable(
                context,
                if (sender) R.drawable.bg_message_send else R.drawable.bg_message_received
            )
        }

        tvMessage.text = messageText

        tvMessage.apply {
            if (chatMessage.message.deleted == true || chatMessage.message.body?.text == context.getString(
                    R.string.deleted_message
                ) || chatMessage.message.type == Const.JsonFields.SYSTEM_TYPE
            ) {
                text = context.getString(R.string.message_deleted_text)
                cvReactedEmoji.visibility = View.GONE
                background =
                    AppCompatResources.getDrawable(
                        context,
                        if (sender) R.drawable.bg_deleted_msg_send else R.drawable.bg_deleted_msg_received
                    )
                setOnLongClickListener { true }
            } else {
                setOnLongClickListener {
                    chatMessage.message.messagePosition = holder.absoluteAdapterPosition
                    onMessageInteraction.invoke(Const.UserActions.MESSAGE_ACTION, chatMessage)
                    true
                }
                movementMethod = LinkMovementMethod.getInstance()

                setOnClickListener {
                    if (chatMessage.message.deliveredCount == -1) {
                        onMessageInteraction.invoke(Const.UserActions.RESEND_MESSAGE, chatMessage)
                    }
                }
            }
        }
    }

    private fun bindSystemMessage(tvMessage: EmojiTextView, msg: MessageAndRecords) {
        tvMessage.text = buildString {
            append(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                    msg.message.createdAt
                ).toString()
            )
            append(" ")
            append(msg.message.body?.text.toString())
        }
    }

    private fun bindAudio(
        holder: ViewHolder,
        chatMessage: MessageAndRecords?,
        audioLayoutBinding: AudioLayoutBinding
    ) {
        with(audioLayoutBinding) {
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
    }

    private fun handleMediaResize(body: MessageBody?): Pair<Int, Int> {
        val imageResized: Pair<Int, Int> = if (body?.thumb != null && body.thumb?.id != 0L) {
            Tools.resizeImage(
                body.thumb?.metaData?.width,
                body.thumb?.metaData?.height
            )
        } else {
            Tools.resizeImage(
                body?.file?.metaData?.width,
                body?.file?.metaData?.height
            )
        }

        return imageResized
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

            view.text = message.createdAt?.let {
                DateUtils.getRelativeTimeSpanString(
                    it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS
                )
            }

        } else {
            view.visibility = View.VISIBLE
            val time = message.createdAt?.let {
                getRelativeTimeSpan(it)
            }

            if (time == context.getString(R.string.zero_minutes_ago) ||
                time == context.getString(R.string.in_zero_minutes)
            ) {
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
