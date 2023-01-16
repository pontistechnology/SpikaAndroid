package com.clover.studio.exampleapp.ui.main.rooms

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.MessageAndRecords
import com.clover.studio.exampleapp.data.models.entity.RoomAndMessageAndRecords
import com.clover.studio.exampleapp.databinding.ItemChatRoomBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
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

                if (roomItem.roomWithUsers.room.muted) {
                    binding.ivMuted.visibility = View.VISIBLE

                    // We need to set margin end programmatically if other views are set to GONE
                    if (binding.ivPinned.visibility == View.GONE && binding.tvNewMessages.visibility == View.GONE) {
                        val params: MarginLayoutParams? =
                            binding.ivMuted.layoutParams as MarginLayoutParams?
                        params?.marginEnd =
                            context.resources.getDimension(R.dimen.twenty_dp_margin).toInt()
                    } else {
                        val params: MarginLayoutParams? =
                            binding.ivMuted.layoutParams as MarginLayoutParams?
                        params?.marginEnd = 0
                    }
                } else binding.ivMuted.visibility = View.GONE

                Glide.with(context)
                    .load(Tools.getFilePathUrl(avatarFileId))
                    .into(binding.ivRoomImage)

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

                    if (time?.get(0) == '0') {
                        binding.tvMessageTime.text = context.getString(R.string.now)
                    } else {
                        binding.tvMessageTime.text = time
                    }

                    val unreadMessages = ArrayList<MessageAndRecords>()
                    if (sortedList.isNotEmpty()) {
                        val filteredMessageList =
                            sortedList.filter { it.message.fromUserId.toString() != myUserId }
                        for (messageAndRecords in filteredMessageList) {
                            if (messageAndRecords.records != null) {
                                if (!checkIfMessageSeen(
                                        roomItem,
                                        messageAndRecords
                                    )
                                ) unreadMessages.add(
                                    messageAndRecords
                                )
                            } else unreadMessages.add(messageAndRecords)
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
