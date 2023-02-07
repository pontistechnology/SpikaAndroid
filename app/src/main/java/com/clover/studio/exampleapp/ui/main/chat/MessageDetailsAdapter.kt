package com.clover.studio.exampleapp.ui.main.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.MessageRecords
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.MessageDetailsItemBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import java.text.SimpleDateFormat
import java.util.*

class MessageDetailsAdapter(
    private val context: Context,
    private val roomWithUsers: RoomWithUsers,
) :
    ListAdapter<MessageRecords, MessageDetailsAdapter.MessageDetailsViewHolder>(ContactsDiffCallback()) {

    inner class MessageDetailsViewHolder(val binding: MessageDetailsItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageDetailsViewHolder {
        val binding =
            MessageDetailsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageDetailsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageDetailsViewHolder, position: Int) {
        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy. HH:mm aa", Locale.getDefault())
        with(holder) {
            getItem(position).let { messageRecord ->
                // Show sender header - this is first and only message record for sender
                if (Const.JsonFields.SENT == messageRecord.type) {
                    binding.tvDetailsHeader.text = context.getString(R.string.sender_actions)
                    binding.ivMessageState.setImageResource(R.drawable.img_sender_actions)
                    binding.tvUserTime.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.img_sent,
                        0
                    )
                    // Show edited information about sender:
                    if (messageRecord.createdAt != messageRecord.modifiedAt) {
                        binding.ivNoEditedTime.visibility = View.GONE
                        val editedTime = simpleDateFormat.format(messageRecord.modifiedAt)
                        binding.tvEditedTime.text = editedTime
                        binding.ivEditedTime.visibility = View.VISIBLE
                        binding.tvEditedTime.visibility = View.VISIBLE
                    } else {
                        binding.ivEditedTime.visibility = View.VISIBLE
                        binding.ivNoEditedTime.visibility = View.VISIBLE
                        binding.tvEditedTime.visibility = View.GONE
                    }
                } else if (Const.JsonFields.SEEN == messageRecord.type) {
                    binding.tvDetailsHeader.text = context.getString(R.string.read_by)
                    binding.ivMessageState.setImageResource(R.drawable.img_seen)
                    binding.tvEditedTime.visibility = View.GONE
                    binding.ivEditedTime.visibility = View.GONE
                    binding.ivNoEditedTime.visibility = View.GONE
                    binding.tvUserTime.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        0,
                        0
                    )
                } else if (Const.JsonFields.DELIVERED == messageRecord.type) {
                    binding.tvDetailsHeader.text = context.getString(R.string.delivered_to)
                    binding.ivMessageState.setImageResource(R.drawable.img_delivered)
                    binding.tvEditedTime.visibility = View.GONE
                    binding.ivEditedTime.visibility = View.GONE
                    binding.ivNoEditedTime.visibility = View.GONE
                    binding.tvUserTime.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        0,
                        0
                    )
                }

                for (user in roomWithUsers.users) {
                    if (messageRecord.userId == user.id) {
                        binding.tvSeenUsername.text = user.displayName
                        Glide.with(context)
                            .load(user.avatarFileId?.let { Tools.getFilePathUrl(it) })
                            .placeholder(AppCompatResources.getDrawable(context, R.drawable.img_user_placeholder))
                            .into(binding.ivUserAvatar)
                        break
                    }
                }

                val dateTime = simpleDateFormat.format(messageRecord.createdAt)
                binding.tvUserTime.text = dateTime

                if (position > 0) {
                    val previousItem = getItem(position - 1).type
                    val currentItem = messageRecord.type

                    if (previousItem == currentItem) {
                        binding.tvDetailsHeader.visibility = View.GONE
                        binding.ivMessageState.visibility = View.GONE
                    } else {
                        binding.tvDetailsHeader.visibility = View.VISIBLE
                        binding.ivMessageState.visibility = View.VISIBLE

                    }
                } else {
                    binding.tvDetailsHeader.visibility = View.VISIBLE
                    binding.ivMessageState.visibility = View.VISIBLE
                }
            }
        }
    }

    private class ContactsDiffCallback : DiffUtil.ItemCallback<MessageRecords>() {
        override fun areItemsTheSame(oldItem: MessageRecords, newItem: MessageRecords) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MessageRecords, newItem: MessageRecords) =
            oldItem == newItem
    }
}