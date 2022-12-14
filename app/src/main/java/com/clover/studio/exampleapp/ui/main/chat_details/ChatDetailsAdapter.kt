package com.clover.studio.exampleapp.ui.main.chat_details

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.databinding.ItemPeopleSelectedBinding
import com.clover.studio.exampleapp.utils.Tools

class ChatDetailsAdapter(
    private val context: Context,
    private val isAdmin: Boolean,
    private val listener: DetailsAdapterListener
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
                binding.tvUsername.text = userItem.displayName
                binding.tvTitle.text = userItem.telephoneNumber
                // Remove first / with substring from avatarUrl
                Glide.with(context).load(userItem.avatarFileId?.let { Tools.getAvatarUrl(it) })
                    .placeholder(context.getDrawable(R.drawable.img_user_placeholder))
                    .into(binding.ivUserImage)

                if (isAdmin) binding.ivRemoveUser.visibility = View.VISIBLE
                else binding.ivRemoveUser.visibility = View.INVISIBLE

                if (userItem.isAdmin) {
                    binding.tvAdmin.visibility = View.VISIBLE
                    binding.ivRemoveUser.visibility = View.INVISIBLE
                } else {
                    binding.tvAdmin.visibility = View.GONE
                }

                itemView.setOnClickListener {
                    userItem.let {
                        listener.onItemClicked(it)
                    }
                }

                binding.ivRemoveUser.setOnClickListener {
                    userItem.let {
                        listener.onViewClicked(position, it)
                    }
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

    interface DetailsAdapterListener {
        fun onItemClicked(user: User)
        fun onViewClicked(position: Int, user: User)
    }
}