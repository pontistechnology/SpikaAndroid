package com.clover.studio.spikamessenger.ui.main.rooms

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.RoomWithMessage
import com.clover.studio.spikamessenger.databinding.ItemChatRoomBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.Tools.getRoomTime
import com.vanniktech.emoji.EmojiTextView
import kotlin.random.Random

const val MAX_UNREAD_MESSAGES = 99

class RoomsAdapter(
    private val context: Context,
    private val myUserId: String,
    private val onItemClick: ((item: RoomWithMessage) -> Unit)
) : ListAdapter<RoomWithMessage, RoomsAdapter.RoomsViewHolder>(RoomsDiffCallback()) {
    inner class RoomsViewHolder(val binding: ItemChatRoomBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomsViewHolder {
        val binding =
            ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomsViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { roomItem ->
                val isPrivateRoom = Const.JsonFields.PRIVATE == roomItem.roomWithUsers.room.type

                val roomUser =
                    if (isPrivateRoom) roomItem.roomWithUsers.users.find { it.id.toString() != myUserId } else null
                val roomName = if (isPrivateRoom) roomUser?.formattedDisplayName
                    ?: "" else roomItem.roomWithUsers.room.name.toString()
                val avatarFileId =
                    if (isPrivateRoom) roomUser?.avatarFileId
                        ?: 0L else roomItem.roomWithUsers.room.avatarFileId ?: 0L

                binding.tvRoomName.text = roomName

                // Check if room is muted and add mute icon to the room item
                binding.ivMuted.visibility = if (roomItem.roomWithUsers.room.muted) {
                    View.VISIBLE
                } else View.GONE

                // Check if room is pinned and add pin icon to the room item
                binding.ivPinned.visibility = if (roomItem.roomWithUsers.room.pinned) {
                    View.VISIBLE
                } else View.GONE

                val placeholderDrawable = Tools.getPlaceholderImage(
                    roomItem.roomWithUsers.room.type!!
                )

                Glide.with(context)
                    .load(Tools.getFilePathUrl(avatarFileId))
                    .placeholder(placeholderDrawable)
                    .centerCrop()
                    .error(placeholderDrawable)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.ivRoomImage)


                if (roomItem.message != null) {
                    val sortedList = roomItem.message
                    val lastMessage = sortedList.body

                    val user =
                        roomItem.roomWithUsers.users.firstOrNull { it.id == sortedList.fromUserId }

                    if (Const.JsonFields.SYSTEM_TYPE == sortedList.type) {
                        // We need to hide the username since it is prefixed to the last message
                        binding.tvUsername.visibility = View.GONE
                    } else {
                        binding.tvUsername.text = if (user?.id.toString() == myUserId) {
                            context.getString(
                                R.string.username_message,
                                context.getString(R.string.you).trim()
                            )
                        } else {
                            context.getString(
                                R.string.username_message,
                                user?.formattedDisplayName?.trim()
                            )
                        }
                        // RV fallback, so it doesn't recycle username hiding
                        binding.tvUsername.visibility = View.VISIBLE
                    }

                    if (lastMessage?.text.isNullOrEmpty()) {
                        setMediaItemText(sortedList, binding.tvLastMessage)
                    } else {
                        binding.tvLastMessage.text = lastMessage?.text.toString()
                        binding.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            0,
                            0
                        )
                    }

                    val time = roomItem.message.createdAt?.let {
                        getRoomTime(it)
                    }

                    // Check for the first digit in the relative time span, if it is a '0' we will
                    // write "Now" instead of the returned time value
                    if (time.toString() == context.getString(R.string.zero_minutes_ago)) {
                        binding.tvMessageTime.text = context.getString(R.string.now)
                    } else {
                        binding.tvMessageTime.text = time
                    }
                } else {
                    binding.tvUsername.text = ""
                    binding.tvMessageTime.text = ""
                    binding.tvLastMessage.text = ""
                    binding.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        0,
                        0
                    )
                    binding.tvNewMessages.visibility = View.GONE
                }

                if (roomItem.roomWithUsers.room.unreadCount > 0 && roomItem.message != null) {
                    val numberOfMessages = roomItem.roomWithUsers.room.unreadCount

                    if (numberOfMessages > MAX_UNREAD_MESSAGES) {
                        binding.tvNewMessages.text = context.getString(R.string.unread_limit)
                    } else {
                        binding.tvNewMessages.text = "${roomItem.roomWithUsers.room.unreadCount + Random.nextInt(6)}"
                    }
                    binding.tvNewMessages.visibility = View.VISIBLE
                } else {
                    binding.tvNewMessages.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    roomItem.let {
                        onItemClick.invoke(it)
                    }
                }
            }
        }
    }

    private fun setMediaItemText(sortedList: Message?, tvLastMessage: EmojiTextView) {
        tvLastMessage.apply {
            val mimeType = sortedList?.body?.file?.mimeType
            val drawableResId = when {
                mimeType?.contains(Const.JsonFields.GIF_TYPE) == true -> R.drawable.img_gif_small
                mimeType?.contains(Const.JsonFields.IMAGE_TYPE) == true -> R.drawable.img_camera_small
                mimeType?.contains(Const.JsonFields.VIDEO_TYPE) == true -> R.drawable.img_video_small
                mimeType?.contains(Const.JsonFields.AUDIO_TYPE) == true -> R.drawable.img_microphone_small
                else -> R.drawable.img_file_small
            }

            setCompoundDrawablesWithIntrinsicBounds(
                drawableResId,
                0,
                0,
                0
            )

            text = if (sortedList?.body?.file?.mimeType.toString().contains(Const.JsonFields.GIF)){
                Const.JsonFields.GIF.replaceFirstChar { it.uppercase() }
            } else {
                sortedList?.type.toString().replaceFirstChar { it.uppercase() }
            }
        }
    }

    private class RoomsDiffCallback : DiffUtil.ItemCallback<RoomWithMessage>() {

        override fun areItemsTheSame(
            oldItem: RoomWithMessage,
            newItem: RoomWithMessage
        ) =
            oldItem.roomWithUsers.room.roomId == newItem.roomWithUsers.room.roomId

        override fun areContentsTheSame(
            oldItem: RoomWithMessage,
            newItem: RoomWithMessage
        ) =
            oldItem == newItem
    }
}
