package com.clover.studio.spikamessenger.ui.main.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.MessageDetailsItemBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools

class MessageDetailsAdapter(
    private val context: Context,
    private val roomWithUsers: RoomWithUsers?,
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
                        binding.tvEditedTime.text = Tools.fullDateFormat(messageRecord.modifiedAt!!)
                        binding.tvEditedTime.visibility = View.VISIBLE
                    } else {
                        binding.tvEditedTime.visibility = View.GONE
                    }
                } else if (Const.JsonFields.SEEN == messageRecord.type) {
                    binding.tvDetailsHeader.text = context.getString(R.string.read_by)
                    binding.ivMessageState.setImageResource(R.drawable.img_seen)
                    binding.tvEditedTime.visibility = View.GONE
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
                    binding.tvUserTime.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        0,
                        0
                    )
                }

                for (user in roomWithUsers!!.users) {
                    if (messageRecord.userId == user.id) {
                        binding.tvSeenUsername.text = user.formattedDisplayName
                        binding.tvPlaceholder.text = user.telephoneNumber
                        Glide.with(context)
                            .load(user.avatarFileId?.let { Tools.getFilePathUrl(it) })
                            .placeholder(
                                AppCompatResources.getDrawable(
                                    context,
                                    R.drawable.img_user_avatar
                                )
                            )
                            .centerCrop()
                            .into(binding.ivUserAvatar)
                        break
                    }
                }

                binding.tvUserTime.text = Tools.fullDateFormat(messageRecord.createdAt)

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
