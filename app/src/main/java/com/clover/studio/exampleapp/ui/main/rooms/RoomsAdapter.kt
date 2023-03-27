package com.clover.studio.exampleapp.ui.main.rooms

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
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.MessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.databinding.ItemChatRoomBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.Tools.getRelativeTimeSpan

class RoomsAdapter(
    private val context: Context,
    private val myUserId: String,
    private val onItemClick: ((item: RoomAndMessageAndRecords) -> Unit)
) : ListAdapter<RoomAndMessageAndRecords, RoomsAdapter.RoomsViewHolder>(RoomsDiffCallback()) {
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
                var userName = ""
                var avatarFileId = 0L
                //Timber.d("Room data = $roomItem, ${roomItem.roomWithUsers.room.name}")
                if (Const.JsonFields.PRIVATE == roomItem.roomWithUsers.room.type) {
                    for (roomUser in roomItem.roomWithUsers.users) {
                        if (myUserId != roomUser.id.toString()) {
                            userName = roomUser.displayName.toString()
                            avatarFileId = roomUser.avatarFileId!!
                            break
                        } else {
                            userName = roomUser.displayName.toString()
                            avatarFileId = roomUser.avatarFileId!!
                        }
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

                if (!roomItem.message.isNullOrEmpty()) {
                    val sortedList = roomItem.message.sortedBy { it.message.createdAt }
                    val lastMessage = sortedList.last().message.body
                    var textUserName = ""

                    if (Const.JsonFields.GROUP == roomItem.roomWithUsers.room.type) {
                        for (user in roomItem.roomWithUsers.users) {
                            if (sortedList.last().message.fromUserId == user.id) {
                                textUserName = user.displayName.toString() + ": "
                                break
                            }
                        }
                    }
                    if (lastMessage?.text.isNullOrEmpty()) {
                        binding.tvLastMessage.text = textUserName + context.getString(
                            R.string.generic_shared,
                            sortedList.last().message.type.toString()
                                .replaceFirstChar { it.uppercase() })
                    } else binding.tvLastMessage.text = textUserName + lastMessage?.text.toString()

                    val time = roomItem.message.last().message.createdAt?.let {
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

                // The second condition handles cases where some rooms have unread counts but have
                // no locally stored messages in them
                if (roomItem.roomWithUsers.room.unreadCount != 0 && roomItem.message?.isNotEmpty() == true) {
                    binding.tvNewMessages.text = roomItem.roomWithUsers.room.unreadCount.toString()
                    binding.tvNewMessages.visibility = View.VISIBLE
                } else binding.tvNewMessages.visibility = View.GONE

                itemView.setOnClickListener {
                    roomItem.let {
                        onItemClick.invoke(it)
                    }
                }
            }
        }
    }


    private class RoomsDiffCallback : DiffUtil.ItemCallback<RoomAndMessageAndRecords>() {

        override fun areItemsTheSame(
            oldItem: RoomAndMessageAndRecords,
            newItem: RoomAndMessageAndRecords
        ) =
            oldItem.roomWithUsers.room.roomId == newItem.roomWithUsers.room.roomId

        override fun areContentsTheSame(
            oldItem: RoomAndMessageAndRecords,
            newItem: RoomAndMessageAndRecords
        ) =
            oldItem == newItem
    }

    private fun checkIfMessageSeen(
        roomItem: RoomAndMessageAndRecords,
        messageAndRecords: MessageAndRecords,
    ): Boolean {

        // This handles case where a message would be sent later to the app via socket since
        // the socket will try to send data until it succeeds.
        if (roomItem.roomWithUsers.room.visitedRoom != null) {
            if (messageAndRecords.message.modifiedAt != null && messageAndRecords.message.modifiedAt <= roomItem.roomWithUsers.room.visitedRoom!!) return true
        }

        if (messageAndRecords.records != null) {
            val myRecords = messageAndRecords.records.filter { it.userId.toString() == myUserId }
            for (record in myRecords) {
                if (record.type == Const.JsonFields.SEEN) {
                    return true
                }
            }
        }
        return false
    }
}
