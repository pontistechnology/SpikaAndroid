package com.clover.studio.exampleapp.ui.main.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        with(holder) {
            getItem(position).let { messageRecord ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = messageRecord.createdAt
                val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
                val dateTime = simpleDateFormat.format(calendar.timeInMillis).toString()
                binding.tvSeenDate.text = dateTime

                if (messageRecord.type == Const.JsonFields.SEEN) {
                    binding.tvDetailsHeader.text = context.getString(R.string.read_by)
                    binding.ivMessageState.setImageResource(R.drawable.img_seen)
                } else {
                    binding.tvDetailsHeader.text = context.getString(R.string.delivered_to)
                    binding.ivMessageState.setImageResource(R.drawable.img_delivered)
                }

                for (user in roomWithUsers.users) {
                    if (messageRecord.userId == user.id) {
                        binding.tvSeenUsername.text = user.displayName
                        Glide.with(context)
                            .load(user.avatarFileId?.let { Tools.getAvatarUrl(it) })
                            .placeholder(context.getDrawable(R.drawable.img_user_placeholder))
                            .into(binding.ivUserAvatar)
                        break
                    }
                }

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