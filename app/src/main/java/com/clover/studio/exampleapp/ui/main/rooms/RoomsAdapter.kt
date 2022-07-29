package com.clover.studio.exampleapp.ui.main.rooms

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.MessageAndRecords
import com.clover.studio.exampleapp.data.models.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.databinding.ItemChatRoomBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools.getAvatarUrl
import com.clover.studio.exampleapp.utils.Tools.getRelativeTimeSpan
import timber.log.Timber

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
                Timber.d("Room data = $roomItem")
                binding.tvRoomName.text = roomItem.roomWithUsers.room.name
                if (Const.JsonFields.PRIVATE == roomItem.roomWithUsers.room.type) {
                    roomItem.roomWithUsers.users.forEach { roomUser ->
                        Timber.d("Room user = ${roomItem.roomWithUsers.room.roomId}, ${roomUser.displayName}")
                        if (myUserId != roomUser.id.toString()) {
                            Glide.with(context)
                                .load(roomUser.avatarUrl?.let { getAvatarUrl(it) })
                                .into(binding.ivRoomImage)
                        }
                    }
                } else {
                    Glide.with(context)
                        .load(roomItem.roomWithUsers.room.avatarUrl?.let { getAvatarUrl(it) })
                        .into(binding.ivRoomImage)
                }

                if (!roomItem.message.isNullOrEmpty()) {
                    val sortedList = roomItem.message.sortedBy { it.message.createdAt }
                    val lastMessage = sortedList.last().message.body
                    if (lastMessage?.text.isNullOrEmpty()) {
                        binding.tvLastMessage.text = context.getString(R.string.image_shared)
                    } else binding.tvLastMessage.text = lastMessage?.text.toString()

                    binding.tvMessageTime.text = roomItem.message.last().message.createdAt?.let {
                        getRelativeTimeSpan(it)
                    }

                    val unreadMessages = ArrayList<MessageAndRecords>()
                    for (messages in sortedList) {
                        if (roomItem.roomWithUsers.room.visitedRoom == null) {
                            unreadMessages.add(messages)
                        } else {
                            if (messages.message.createdAt!! >= roomItem.roomWithUsers.room.visitedRoom!!) {
                                unreadMessages.add(messages)
                            }
                        }
                    }

                    if (unreadMessages.isNotEmpty()) {
                        binding.tvNewMessages.text = unreadMessages.size.toString()
                        binding.tvNewMessages.visibility = View.VISIBLE
                    } else binding.tvNewMessages.visibility = View.GONE
                } else {
                    binding.tvLastMessage.text = ""
                    binding.tvMessageTime.text = ""
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
}