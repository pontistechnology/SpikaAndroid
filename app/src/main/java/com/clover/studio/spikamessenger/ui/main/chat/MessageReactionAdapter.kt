package com.clover.studio.spikamessenger.ui.main.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.ReactionsDetailsItemBinding
import com.clover.studio.spikamessenger.utils.Tools

class MessageReactionAdapter(
    private val context: Context,
    private val roomWithUsers: RoomWithUsers,
    private val localUserId: Int,
    private val deleteReaction: ((reactionToDelete: MessageRecords?) -> Unit),
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
        val item = getItem(position)
        val user = roomWithUsers.users.find { user -> user.id == item.userId }

        if (user != null) {
            holder.binding.tvUserReaction.text = item.reaction
            holder.binding.tvUsernameReaction.text = user.formattedDisplayName
            Glide.with(context)
                .load(user.avatarFileId?.let { Tools.getFilePathUrl(it) })
                .placeholder(R.drawable.img_user_avatar)
                .dontTransform()
                .dontAnimate()
                .centerCrop()
                .into(holder.binding.ivUserReactionAvatar)

            val isLocalUser = user.id == localUserId
            holder.binding.tvTapToRemove.visibility = if (isLocalUser) View.VISIBLE else View.GONE
            holder.binding.tvUserNumber.visibility = if (isLocalUser) View.GONE else View.VISIBLE

            if (isLocalUser) {
                holder.binding.clReactedContainer.setOnClickListener { _ ->
                    deleteReaction(item)
                }
            } else {
                holder.binding.tvUserNumber.text = user.telephoneNumber.toString()
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
