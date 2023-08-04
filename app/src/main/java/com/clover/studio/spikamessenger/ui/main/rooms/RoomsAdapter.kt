package com.clover.studio.spikamessenger.ui.main.rooms

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.RoomWithMessage
import com.clover.studio.spikamessenger.databinding.ItemChatRoomBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.Tools.getRelativeTimeSpan

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
                val userName: String
                val avatarFileId: Long
                //Timber.d("Room data = $roomItem, ${roomItem.roomWithUsers.room.name}")
                if (Const.JsonFields.PRIVATE == roomItem.roomWithUsers.room.type) {
                    val roomUser = roomItem.roomWithUsers.users.find { it.id.toString() != myUserId }
                    if (roomUser != null) {
                        userName = roomUser.formattedDisplayName
                        avatarFileId = roomUser.avatarFileId!!
                    } else {
                        userName = roomItem.roomWithUsers.users.first().formattedDisplayName
                        avatarFileId = roomItem.roomWithUsers.users.first().avatarFileId!!
                    }
                } else {
                    userName = roomItem.roomWithUsers.room.name.toString()
                    avatarFileId = roomItem.roomWithUsers.room.avatarFileId!!
                }
                binding.tvRoomName.text = userName

                // Check if room is muted and add mute icon to the room item
                if (roomItem.roomWithUsers.room.muted) {
                    binding.ivMuted.visibility = View.VISIBLE
                } else binding.ivMuted.visibility = View.GONE

                // Check if room is pinned and add pin icon to the room item
                if (roomItem.roomWithUsers.room.pinned) {
                    binding.ivPinned.visibility = View.VISIBLE
                } else binding.ivPinned.visibility = View.GONE

                if (avatarFileId != 0L) {
                    Glide.with(context)
                        .load(Tools.getFilePathUrl(avatarFileId))
                        .placeholder(R.drawable.img_user_placeholder)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.ivRoomImage)
                } else binding.ivRoomImage.setImageDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.img_user_placeholder
                    )
                )

                if (roomItem.message != null) {
                    val sortedList = roomItem.message
                    val lastMessage = sortedList.body
                    var textUserName = ""

                    if (Const.JsonFields.GROUP == roomItem.roomWithUsers.room.type) {
                        for (user in roomItem.roomWithUsers.users) {
                            if (sortedList.fromUserId == user.id) {
                                textUserName = user.formattedDisplayName + ": "
                                break
                            }
                        }
                    }
                    if (lastMessage?.text.isNullOrEmpty()) {
                        binding.tvLastMessage.text = buildString {
                            append(textUserName)
                            append(
                                context.getString(
                                    R.string.generic_shared,
                                    sortedList.type.toString()
                                        .replaceFirstChar { it.uppercase() })
                            )
                        }
                    } else binding.tvLastMessage.text = buildString {
                        append(textUserName)
                        append(lastMessage?.text.toString())
                    }

                    val time = roomItem.message.createdAt?.let {
                        getRelativeTimeSpan(it)
                    }

                    // Check for the first digit in the relative time span, if it is a '0' we will
                    // write "Now" instead of the returned time value
                    if (time?.firstOrNull { it.isDigit() }?.equals('0') == true) {
                        binding.tvMessageTime.text = context.getString(R.string.now)
                    } else {
                        binding.tvMessageTime.text = time
                    }
                } else {
                    binding.tvLastMessage.text = ""
                    binding.tvMessageTime.text = ""
                    binding.tvNewMessages.visibility = View.GONE
                }

                if (roomItem.roomWithUsers.room.unreadCount > 0 && roomItem.message != null) {
                    binding.tvNewMessages.text = roomItem.roomWithUsers.room.unreadCount.toString()
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
