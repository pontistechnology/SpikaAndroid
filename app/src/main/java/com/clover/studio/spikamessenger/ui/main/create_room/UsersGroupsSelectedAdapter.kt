package com.clover.studio.spikamessenger.ui.main.create_room

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import com.clover.studio.spikamessenger.databinding.ItemNewRoomContactBinding
import com.clover.studio.spikamessenger.utils.Tools

class UsersGroupsSelectedAdapter(
    private val context: Context,
    private val onItemClick: ((item: PrivateGroupChats) -> Unit)
) :
    ListAdapter<PrivateGroupChats, UsersGroupsSelectedAdapter.ContactsViewHolder>(ContactsDiffCallback()) {

    inner class ContactsViewHolder(val binding: ItemNewRoomContactBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val binding =
            ItemNewRoomContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        with(holder) {
            getItem(position).let {
                
                // Chat name
                val displayName = if (it.phoneNumber != null) {
                    it.userName ?: it.userPhoneName.toString()
                } else {
                    it.roomName.toString()
                }
                binding.tvUserName.text =
                    if (displayName.length > 10) "${displayName.take(10)}..." else displayName

                Glide.with(context)
                    .load(Tools.getFilePathUrl(it.avatarId))
                    .placeholder(R.drawable.img_user_avatar)
                    .error(R.drawable.img_user_avatar)
                    .centerCrop()
                    .into(binding.ivUserImage)

                itemView.setOnClickListener { _ ->
                    it.let {
                        onItemClick.invoke(it)
                    }
                }

                binding.ivRemove.setOnClickListener { _ ->
                    it.let {
                        onItemClick.invoke(it)
                    }
                }
            }
        }
    }

    private class ContactsDiffCallback : DiffUtil.ItemCallback<PrivateGroupChats>() {
        override fun areItemsTheSame(oldItem: PrivateGroupChats, newItem: PrivateGroupChats) =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: PrivateGroupChats, newItem: PrivateGroupChats) =
            oldItem == newItem
    }
}
