package com.clover.studio.exampleapp.ui.main.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.MessageRecords
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.ReactionsDetailsItemBinding
import com.clover.studio.exampleapp.utils.Tools

class MessageReactionAdapter(
    private val context: Context,
    private val roomWithUsers: RoomWithUsers,
) :
    ListAdapter<MessageRecords, MessageReactionAdapter.MessageReactionViewHolder>(
        ContactsDiffCallback()
    ) {

    inner class MessageReactionViewHolder(val binding: ReactionsDetailsItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageReactionViewHolder {
        val binding =
            ReactionsDetailsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageReactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageReactionViewHolder, position: Int) {
        getItem(position).let {
            holder.binding.tvUserReaction.text = it.reaction
            for (user in roomWithUsers.users) {
                if (it.userId == user.id) {
                    holder.binding.tvUsernameReaction.text = user.formattedDisplayName
                    Glide.with(context)
                        .load(user.avatarFileId?.let { fileId -> Tools.getFilePathUrl(fileId) })
                        .placeholder(R.drawable.img_user_placeholder)
                        .dontTransform()
                        .dontAnimate()
                        .centerCrop()
                        .into(holder.binding.ivUserReactionAvatar)
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