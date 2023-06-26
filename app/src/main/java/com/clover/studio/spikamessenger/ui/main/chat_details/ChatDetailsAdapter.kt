package com.clover.studio.spikamessenger.ui.main.chat_details

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.databinding.ItemPeopleSelectedBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools

class ChatDetailsAdapter(
    private val context: Context,
    private val isAdmin: Boolean,
    private val roomType: String,
    private val onUserInteraction: ((event: String, user: User) -> Unit)
) :
    ListAdapter<User, ChatDetailsAdapter.ChatDetailsViewHolder>(ChatDetailsCallback()) {

    inner class ChatDetailsViewHolder(val binding: ItemPeopleSelectedBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatDetailsViewHolder {
        val binding =
            ItemPeopleSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatDetailsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatDetailsViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { userItem ->
                binding.tvUsername.text = userItem.formattedDisplayName
                binding.tvTitle.text = userItem.telephoneNumber
                Glide.with(context).load(userItem.avatarFileId?.let { Tools.getFilePathUrl(it) })
                    .placeholder(R.drawable.img_user_placeholder)
                    .centerCrop()
                    .into(binding.ivUserImage)

                if (Const.JsonFields.PRIVATE == roomType) {
                    binding.ivRemoveUser.visibility = View.GONE
                    binding.tvAdmin.visibility = View.GONE
                } else {
                    binding.ivRemoveUser.visibility = if (isAdmin) View.VISIBLE else View.INVISIBLE
                    if (userItem.isAdmin) {
                        binding.tvAdmin.visibility = View.VISIBLE
                        binding.ivRemoveUser.visibility = View.INVISIBLE
                    } else {
                        binding.tvAdmin.visibility = View.GONE
                    }
                }

                itemView.setOnClickListener {
                    onUserInteraction(Const.UserActions.USER_OPTIONS, userItem)
                }

                binding.ivRemoveUser.setOnClickListener {
                    onUserInteraction(Const.UserActions.USER_REMOVE, userItem)
                }
            }
        }
    }

    private class ChatDetailsCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: User, newItem: User) =
            oldItem == newItem
    }
}