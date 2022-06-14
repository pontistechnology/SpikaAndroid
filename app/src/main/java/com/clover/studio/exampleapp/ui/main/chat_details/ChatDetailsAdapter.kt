package com.clover.studio.exampleapp.ui.main.chat_details

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.ItemPeopleSelectedBinding
import com.clover.studio.exampleapp.utils.Tools
import timber.log.Timber

class ChatDetailsAdapter(
    private val context: Context,
    private val onItemClick: ((item: User) -> Unit)
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
                Timber.d("Adapter user item = $userItem")
                Timber.d("Adapter user item names = ${userItem.displayName}")
                binding.tvUsername.text = userItem.displayName
                binding.tvTitle.text = userItem.telephoneNumber
                // Remove first / with substring from avatarUrl
                Glide.with(context).load(userItem.avatarUrl?.let { Tools.getAvatarUrl(it) })
                    .into(binding.ivUserImage)

                itemView.setOnClickListener {
                    userItem.let {
                        onItemClick.invoke(it)
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
}