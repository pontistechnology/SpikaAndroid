package com.clover.studio.exampleapp.ui.main.chat

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.entity.MessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.MessageRecords
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
private const val TEXT_SIZE_BIG = 11
private const val TEXT_SIZE_SMALL = 5
private const val MAX_REACTIONS = 3
private var oldPosition = -1
private var firstPlay = true

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
    private var exoPlayer: ExoPlayer,
    private var roomType: String?,
    private val onMessageInteraction: ((event: String, message: MessageAndRecords) -> Unit)
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

    private var handler = Handler(Looper.getMainLooper())

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position).let { it ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it.message.createdAt!!
            val date = calendar.get(Calendar.DAY_OF_MONTH)

            // View holder for my messages
            // TODO can two view holders use same method for binding if all views are the same?
            if (holder.itemViewType == VIEW_TYPE_MESSAGE_SENT) {
                holder as SentMessageHolder
                holder.binding.clContainer.setBackgroundResource(R.drawable.bg_btn_white)
                holder.binding.tvTime.visibility = View.GONE
                when (it.message.type) {
                    Const.JsonFields.TEXT_TYPE -> {
                        holder.binding.tvMessage.text = it.message.body?.text
                        holder.binding.tvMessage.visibility = View.VISIBLE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        // Code below removes click listener if message was media before
                        // being deleted
                        holder.binding.clContainer.setOnClickListener {
                            // ignore
                        }

                        holder.binding.tvMessage.setOnClickListener {
                            if (holder.binding.tvTime.visibility == View.GONE) {
                                holder.binding.tvTime.visibility = View.VISIBLE
                                val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val dateTime = simpleDateFormat.format(calendar.timeInMillis).toString()
                                holder.binding.tvTime.text = dateTime
                            } else {
                                holder.binding.tvTime.visibility = View.GONE
                            }
                        }

                        holder.binding.tvMessage.movementMethod = LinkMovementMethod.getInstance()
                        holder.binding.tvMessage.setOnLongClickListener { _ ->
                            it.message.senderMessage = true
                            it.message.messagePosition = holder.absoluteAdapterPosition
                            onMessageInteraction.invoke(Const.UserActions.MESSAGE_ACTION, it)
                            true
                        }
                    }
                    Const.JsonFields.IMAGE_TYPE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.VISIBLE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        if (it.message.body?.file?.uri != null) {
                            holder.binding.clProgressScreen.visibility = View.VISIBLE
                            Glide.with(context)
                                .load(it.message.body.file?.uri)
                                .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                                .placeholder(R.drawable.img_image_placeholder)
                                .dontTransform()
                                .dontAnimate()
                                .into(holder.binding.ivChatImage)
                        } else {
                            holder.binding.clProgressScreen.visibility = View.GONE
                            val imagePath = it.message.body?.thumb?.id?.let { imagePath ->
                                Tools.getFilePathUrl(
                                    imagePath
                                )
                            }

                            val imageFile = it.message.body?.fileId?.let { imageFile ->
                                Tools.getFilePathUrl(imageFile)
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
                                        "", imageFile!!
                                    )
                                view.findNavController().navigate(action)
                            }
                        }
                    }
                    Const.JsonFields.FILE_TYPE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.VISIBLE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        holder.binding.tvFileTitle.text = it.message.body?.file?.fileName
                        val sizeText =
                            Tools.calculateFileSize(it.message.body?.file?.size!!)
                                .toString()
                        holder.binding.tvFileSize.text = sizeText

                        if (it.message.body.file?.uri?.isEmpty() == true) {
                            holder.binding.ivDownloadFile.visibility = View.GONE
                            holder.binding.ivCancelFile.visibility = View.VISIBLE
                            holder.binding.pbFile.visibility = View.VISIBLE
                            holder.binding.ivCancelFile.setOnClickListener { _ ->
                                onMessageInteraction(Const.UserActions.DOWNLOAD_CANCEL, it)
                            }
                        } else {
                            holder.binding.ivDownloadFile.visibility = View.VISIBLE
                            addFiles(it.message, holder.binding.ivFileType)
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
                    }
                    Const.JsonFields.VIDEO_TYPE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        val videoThumb = it.message.body?.thumb?.id?.let { videoThumb ->
                            Tools.getFilePathUrl(
                                videoThumb
                            )
                        }

                        val videoPath = it.message.body?.file?.id?.let { videoPath ->
                            Tools.getFilePathUrl(videoPath)
                        }

                        Glide.with(context)
                            .load(videoThumb)
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
                    Const.JsonFields.AUDIO_TYPE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.VISIBLE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        if (it.message.body?.file?.uri?.isEmpty() == true) {
                            holder.binding.pbAudio.visibility = View.VISIBLE
                            holder.binding.ivPlayAudio.visibility = View.GONE
                            holder.binding.ivCancelAudio.visibility = View.GONE
                            holder.binding.ivCancelAudio.setOnClickListener { _ ->
                                onMessageInteraction(Const.UserActions.DOWNLOAD_CANCEL, it)
                            }
                        } else {
                            holder.binding.ivPlayAudio.visibility = View.VISIBLE
                            holder.binding.pbAudio.visibility = View.GONE
                            holder.binding.ivCancelAudio.visibility = View.GONE
                            val audioPath = it.message.body?.file?.id?.let { audioPath ->
                                Tools.getFilePathUrl(
                                    audioPath
                                )
                            }

                            val mediaItem: MediaItem = MediaItem.fromUri(Uri.parse(audioPath))
                            exoPlayer.clearMediaItems()
                            holder.binding.sbAudio.progress = 0

                            val runnable = object : Runnable {
                                override fun run() {
                                    holder.binding.sbAudio.progress =
                                        exoPlayer.currentPosition.toInt()
                                    holder.binding.tvAudioDuration.text =
                                        Tools.convertDurationMillis(exoPlayer.currentPosition)
                                    handler.postDelayed(this, 100)
                                }
                            }

                            holder.binding.ivPlayAudio.setOnClickListener {
                                if (!exoPlayer.isPlaying) {
                                    if (oldPosition != holder.absoluteAdapterPosition) {
                                        firstPlay = true
                                        exoPlayer.stop()
                                        exoPlayer.clearMediaItems()
                                        holder.binding.tvAudioDuration.text =
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
                                    holder.binding.ivPlayAudio.setImageResource(R.drawable.img_pause_audio_button)
                                } else {
                                    holder.binding.ivPlayAudio.setImageResource(R.drawable.img_play_audio_button)
                                    exoPlayer.pause()
                                    firstPlay = false
                                    handler.removeCallbacks(runnable)
                                }
                            }

                            exoPlayer.addListener(object : Player.Listener {
                                override fun onPlaybackStateChanged(state: Int) {
                                    if (state == Player.STATE_READY) {
                                        holder.binding.sbAudio.max = exoPlayer.duration.toInt()
                                    }
                                    if (state == Player.STATE_ENDED) {
                                        holder.binding.ivPlayAudio.visibility = View.VISIBLE
                                        firstPlay = true
                                        exoPlayer.pause()
                                        exoPlayer.clearMediaItems()
                                        handler.removeCallbacks(runnable)
                                        holder.binding.tvAudioDuration.text =
                                            context.getString(R.string.audio_duration)
                                        holder.binding.ivPlayAudio.setImageResource(R.drawable.img_play_audio_button)
                                    }
                                }
                            })

                            // Seek through audio
                            holder.binding.sbAudio.setOnSeekBarChangeListener(object :
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
                    }

                    else -> {
                        holder.binding.tvMessage.visibility = View.VISIBLE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE
                    }
                }

                // Reply section
                holder.binding.ivReplyImage.visibility = View.GONE
                holder.binding.tvReplyMedia.visibility = View.GONE
                holder.binding.tvMessageReply.visibility = View.GONE
                if (it.message.replyId != null && it.message.replyId != 0L) {
                    val params =
                        holder.binding.clReplyMessage.layoutParams as ConstraintLayout.LayoutParams
                    params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    val original = it.message.body?.text?.length
                    holder.binding.clReplyMessage.visibility = View.VISIBLE
                    holder.binding.clContainer.setBackgroundResource(R.drawable.bg_message_user)
                    for (roomUser in users) {
                        if (it.message.body?.referenceMessage?.fromUserId == roomUser.id) {
                            holder.binding.tvUsername.text = roomUser.displayName
                            break
                        }
                    }
                    when (it.message.body?.referenceMessage?.type) {
                        Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE -> {
                            if (original!! >= TEXT_SIZE_BIG) {
                                params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                            }
                            val imagePath =
                                it.message.body.referenceMessage?.body?.thumbId?.let { imagePath ->
                                    Tools.getFilePathUrl(
                                        imagePath
                                    )
                                }
                            holder.binding.tvMessageReply.visibility = View.GONE
                            holder.binding.ivReplyImage.visibility = View.VISIBLE
                            holder.binding.tvReplyMedia.visibility = View.VISIBLE
                            if (it.message.body.referenceMessage?.type == Const.JsonFields.IMAGE_TYPE) {
                                holder.binding.tvReplyMedia.text = context.getString(
                                    R.string.media,
                                    context.getString(R.string.photo)
                                )
                                holder.binding.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.img_camera_reply,
                                    0,
                                    0,
                                    0
                                )
                            } else {
                                holder.binding.tvReplyMedia.text = context.getString(
                                    R.string.media,
                                    context.getString(R.string.video)
                                )
                                holder.binding.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.img_video_reply,
                                    0,
                                    0,
                                    0
                                )
                            }
                            Glide.with(context)
                                .load(imagePath)
                                .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                                .placeholder(R.drawable.img_image_placeholder)
                                .dontTransform()
                                .dontAnimate()
                                .into(holder.binding.ivReplyImage)
                        }
                        Const.JsonFields.AUDIO_TYPE -> {
                            if (original!! >= TEXT_SIZE_SMALL) {
                                params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                            }
                            holder.binding.tvMessageReply.visibility = View.GONE
                            holder.binding.ivReplyImage.visibility = View.GONE
                            holder.binding.tvReplyMedia.visibility = View.VISIBLE
                            holder.binding.tvReplyMedia.text =
                                context.getString(R.string.media, context.getString(R.string.audio))
                            holder.binding.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
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
                            holder.binding.tvMessageReply.visibility = View.GONE
                            holder.binding.tvReplyMedia.visibility = View.VISIBLE
                            holder.binding.tvReplyMedia.text =
                                context.getString(R.string.media, context.getString(R.string.file))
                            holder.binding.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.img_file_reply,
                                0,
                                0,
                                0
                            )
                        }
                        else -> {
                            holder.binding.tvMessageReply.visibility = View.VISIBLE
                            holder.binding.ivReplyImage.visibility = View.GONE
                            holder.binding.tvReplyMedia.visibility = View.GONE
                            val replyText = it.message.body?.referenceMessage?.body?.text
                            holder.binding.tvMessageReply.text = replyText

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
                val reactionList = it.records!!.sortedByDescending { it.createdAt }
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
                        holder.binding.tvReactedEmoji.text = spanStringBuilder.append(" ")
                    } else {
                        holder.binding.tvReactedEmoji.text = reactionText
                    }
                    holder.binding.cvReactedEmoji.visibility = View.VISIBLE
                } else {
                    holder.binding.cvReactedEmoji.visibility = View.GONE
                }

                holder.binding.cvReactedEmoji.setOnClickListener { _ ->
                    onMessageInteraction.invoke(Const.UserActions.SHOW_MESSAGE_REACTIONS, it)
                }

                // Send new reaction:
                holder.binding.clContainer.setOnLongClickListener { _ ->
                    it.message.senderMessage = true
                    it.message.messagePosition = holder.absoluteAdapterPosition
                    onMessageInteraction.invoke(Const.UserActions.MESSAGE_ACTION, it)
                    true
                }

                showDateHeader(position, date, holder.binding.tvSectionHeader, it.message)

                when {
                    it.message.totalUserCount == it.message.seenCount!! -> {
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
                holder.binding.clContainer.setBackgroundResource(R.drawable.bg_message_received)
                holder.binding.tvTime.visibility = View.GONE
                when (it.message.type) {
                    Const.JsonFields.TEXT_TYPE -> {
                        holder.binding.tvMessage.text = it.message.body?.text
                        holder.binding.tvMessage.visibility = View.VISIBLE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        holder.binding.tvMessage.setOnClickListener {
                            if (holder.binding.tvTime.visibility == View.GONE) {
                                holder.binding.tvTime.visibility = View.VISIBLE
                                val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                val dateTime = simpleDateFormat.format(calendar.timeInMillis).toString()
                                holder.binding.tvTime.text = dateTime
                            } else {
                                holder.binding.tvTime.visibility = View.GONE
                            }
                        }

                        holder.binding.tvMessage.movementMethod = LinkMovementMethod.getInstance()
                        holder.binding.tvMessage.setOnLongClickListener { _ ->
                            it.message.senderMessage = false
                            it.message.messagePosition = holder.absoluteAdapterPosition
                            onMessageInteraction.invoke(Const.UserActions.MESSAGE_ACTION, it)
                            true
                        }
                    }
                    Const.JsonFields.IMAGE_TYPE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.VISIBLE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        val imagePath = it.message.body?.thumb?.id?.let { imagePath ->
                            Tools.getFilePathUrl(
                                imagePath
                            )
                        }

                        val imageFile = it.message.body?.fileId?.let { imageFile ->
                            Tools.getFilePathUrl(imageFile)
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
                                    "", imageFile!!
                                )
                            view.findNavController().navigate(action)
                        }
                    }
                    Const.JsonFields.VIDEO_TYPE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.VISIBLE
                        holder.binding.cvAudio.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        val videoThumb = it.message.body?.thumb?.id?.let { videoThumb ->
                            Tools.getFilePathUrl(
                                videoThumb
                            )
                        }

                        val videoPath = it.message.body?.file?.id.let { videoPath ->
                            Tools.getFilePathUrl(
                                videoPath!!
                            )
                        }
                        Glide.with(context)
                            .load(videoThumb)
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
                                    videoPath, ""
                                )
                            view.findNavController().navigate(action)
                        }
                    }
                    Const.JsonFields.FILE_TYPE -> {
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.VISIBLE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        holder.binding.tvFileTitle.text = it.message.body?.file?.fileName
                        val sizeText =
                            Tools.calculateFileSize(it.message.body?.file?.size!!)
                                .toString()
                        holder.binding.tvFileSize.text = sizeText

                        addFiles(it.message, holder.binding.ivFileType)

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
                        holder.binding.tvMessage.visibility = View.GONE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.cvAudio.visibility = View.VISIBLE
                        holder.binding.clReplyMessage.visibility = View.GONE

                        val audioPath = it.message.body?.file?.id?.let { audioPath ->
                            Tools.getFilePathUrl(
                                audioPath
                            )
                        }

                        val mediaItem: MediaItem = MediaItem.fromUri(Uri.parse(audioPath))
                        exoPlayer.clearMediaItems()
                        holder.binding.sbAudio.progress = 0

                        val runnable = object : Runnable {
                            override fun run() {
                                holder.binding.sbAudio.progress = exoPlayer.currentPosition.toInt()
                                holder.binding.tvAudioDuration.text =
                                    Tools.convertDurationMillis(exoPlayer.currentPosition)
                                handler.postDelayed(this, 100)
                            }
                        }

                        holder.binding.ivPlayAudio.setOnClickListener {
                            if (!exoPlayer.isPlaying) {
                                if (oldPosition != holder.absoluteAdapterPosition) {
                                    firstPlay = true
                                    exoPlayer.stop()
                                    exoPlayer.clearMediaItems()
                                    holder.binding.tvAudioDuration.text =
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
                                holder.binding.ivPlayAudio.setImageResource(R.drawable.img_pause_audio_button)
                            } else {
                                holder.binding.ivPlayAudio.setImageResource(R.drawable.img_play_audio_button)
                                exoPlayer.pause()
                                firstPlay = false
                                handler.removeCallbacks(runnable)
                            }
                        }

                        exoPlayer.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                if (state == Player.STATE_READY) {
                                    holder.binding.sbAudio.max = exoPlayer.duration.toInt()
                                }
                                if (state == Player.STATE_ENDED) {
                                    holder.binding.ivPlayAudio.visibility = View.VISIBLE
                                    firstPlay = true
                                    exoPlayer.pause()
                                    exoPlayer.clearMediaItems()
                                    handler.removeCallbacks(runnable)
                                    holder.binding.tvAudioDuration.text =
                                        context.getString(R.string.audio_duration)
                                    holder.binding.ivPlayAudio.setImageResource(R.drawable.img_play_audio_button)
                                }
                            }
                        })

                        // Seek through audio
                        holder.binding.sbAudio.setOnSeekBarChangeListener(object :
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
                    else -> {
                        holder.binding.tvMessage.visibility = View.VISIBLE
                        holder.binding.cvImage.visibility = View.GONE
                        holder.binding.clFileMessage.visibility = View.GONE
                        holder.binding.clVideos.visibility = View.GONE
                        holder.binding.clReplyMessage.visibility = View.GONE
                    }
                }

                // Reply section
                if (it.message.replyId != null && it.message.replyId != 0L) {
                    val params =
                        holder.binding.clReplyMessage.layoutParams as ConstraintLayout.LayoutParams
                    params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    val original = it.message.body?.text?.length
                    holder.binding.clReplyMessage.visibility = View.VISIBLE
                    holder.binding.clContainer.setBackgroundResource(R.drawable.bg_message_received)
                    for (roomUser in users) {
                        if (it.message.body?.referenceMessage?.fromUserId == roomUser.id) {
                            holder.binding.tvUsernameOther.text = roomUser.displayName.toString()
                            break
                        }
                    }
                    when (it.message.body?.referenceMessage?.type) {
                        Const.JsonFields.IMAGE_TYPE, Const.JsonFields.VIDEO_TYPE -> {
                            if (original!! >= TEXT_SIZE_BIG) {
                                params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                            }
                            holder.binding.tvMessageReply.visibility = View.GONE
                            holder.binding.cvReplyMedia.visibility = View.VISIBLE
                            holder.binding.tvReplyMedia.visibility = View.VISIBLE
                            val imagePath =
                                it.message.body.referenceMessage?.body?.thumbId?.let { imagePath ->
                                    Tools.getFilePathUrl(
                                        imagePath
                                    )
                                }
                            if (it.message.body.referenceMessage?.type == Const.JsonFields.IMAGE_TYPE) {
                                holder.binding.tvReplyMedia.text = context.getString(
                                    R.string.media,
                                    context.getString(R.string.photo)
                                )
                                holder.binding.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.img_camera_reply,
                                    0,
                                    0,
                                    0
                                )
                            } else {
                                holder.binding.tvReplyMedia.text = context.getString(
                                    R.string.media,
                                    context.getString(R.string.video)
                                )
                                holder.binding.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.img_video_reply,
                                    0,
                                    0,
                                    0
                                )
                            }
                            Timber.d("Image path = $imagePath")
                            Glide.with(context)
                                .load(imagePath)
                                .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                                .placeholder(R.drawable.img_image_placeholder)
                                .dontTransform()
                                .dontAnimate()
                                .into(holder.binding.ivReplyImage)
                        }
                        Const.JsonFields.AUDIO_TYPE -> {
                            if (original!! >= TEXT_SIZE_SMALL) {
                                params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                            }
                            holder.binding.tvMessageReply.visibility = View.GONE
                            holder.binding.cvReplyMedia.visibility = View.GONE
                            holder.binding.tvReplyMedia.visibility = View.VISIBLE
                            holder.binding.tvReplyMedia.text =
                                context.getString(R.string.media, context.getString(R.string.audio))
                            holder.binding.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
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
                            holder.binding.tvMessageReply.visibility = View.GONE
                            holder.binding.tvReplyMedia.visibility = View.VISIBLE
                            holder.binding.tvReplyMedia.text =
                                context.getString(R.string.media, context.getString(R.string.file))
                            holder.binding.tvReplyMedia.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.img_file_reply,
                                0,
                                0,
                                0
                            )
                        }

                        else -> {
                            holder.binding.tvMessageReply.visibility = View.VISIBLE
                            holder.binding.cvReplyMedia.visibility = View.GONE
                            holder.binding.tvReplyMedia.visibility = View.GONE
                            val replyText = it.message.body?.referenceMessage?.body?.text
                            holder.binding.tvMessageReply.text = replyText

                            // Check which layout is wider
                            val reply = replyText?.length
                            if (original != null && reply != null) {
                                if (original > reply && original >= TEXT_SIZE_SMALL) {
                                    params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                                }
                            }
                        }
                    }
                }

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
                                .placeholder(AppCompatResources.getDrawable(context, R.drawable.img_user_placeholder))
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
                val reactionList = it.records!!.sortedByDescending { it.createdAt }
                val reactionText = getDatabaseReaction(reactionList)

                // Show reactions if there are any in the database
                if (reactionText.isNotEmpty()) {
                    if (reactionText.last().isDigit()) {
                        val spanStringBuilder = SpannableStringBuilder(reactionText)
                        spanStringBuilder.setSpan(
                            RelativeSizeSpan(0.5f),
                            reactionText.length - 2,
                            reactionText.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        holder.binding.tvReactedEmoji.text = spanStringBuilder.append(" ")
                    } else {
                        holder.binding.tvReactedEmoji.text = reactionText
                    }
                    holder.binding.cvReactedEmoji.visibility = View.VISIBLE
                } else {
                    holder.binding.cvReactedEmoji.visibility = View.GONE
                }

                // Send new reaction:
                holder.binding.clContainer.setOnLongClickListener { _ ->
                    it.message.senderMessage = false
                    it.message.messagePosition = holder.absoluteAdapterPosition
                    onMessageInteraction.invoke(Const.UserActions.MESSAGE_ACTION, it)
                    true
                }

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

    /** Rules:
    - there can be a maximum of 3 reactions
    - if there are more, then the total number is displayed next to it
    - if there are a total of three reactions and 2 of those 3 are equal, then two reactions and the total number are shown next to (3)
    - if there are less than three reactions, then both are shown
    - reactions are placed in the order in which they are placed, which means that the newer reactions will push out the older ones if there are more than three of them
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

            val time = message.createdAt?.let {
                DateUtils.getRelativeTimeSpanString(
                    it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS
                )
            }

            if (time?.equals(context.getString(R.string.zero_ninutes_ago)) == true) {
                view.text = context.getString(R.string.now)
            } else {
                view.text = time
            }
        } else {
            view.visibility = View.VISIBLE
            val time = message.createdAt?.let {
                getRelativeTimeSpan(it)
            }

            if (time?.equals(context.getString(R.string.zero_ninutes_ago)) == true) {
                view.text = context.getString(R.string.now)
            } else {
                view.text = time
            }
        }

    }

}