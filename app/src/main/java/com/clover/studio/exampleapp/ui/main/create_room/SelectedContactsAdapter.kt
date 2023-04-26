package com.clover.studio.exampleapp.ui.main.create_room

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.UserAndPhoneUser
import com.clover.studio.exampleapp.databinding.ItemNewRoomContactBinding
import com.clover.studio.exampleapp.utils.Tools

class SelectedContactsAdapter(
    private val context: Context,
    private val onItemClick: ((item: UserAndPhoneUser) -> Unit)
) :
    ListAdapter<UserAndPhoneUser, SelectedContactsAdapter.ContactsViewHolder>(ContactsDiffCallback()) {

    inner class ContactsViewHolder(val binding: ItemNewRoomContactBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val binding =
            ItemNewRoomContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { userItem ->
                binding.tvUserName.text = userItem.phoneUser?.name ?: userItem.user.displayName
                Glide.with(context)
                    .load(userItem.user.avatarFileId?.let { Tools.getFilePathUrl(it) })
                    .placeholder(R.drawable.img_user_placeholder)
                    .centerCrop()
                    .into(binding.ivUserImage)

                itemView.setOnClickListener {
                    userItem.let {
                        onItemClick.invoke(it)
                    }
                }
            }
        }
    }

    private class ContactsDiffCallback : DiffUtil.ItemCallback<UserAndPhoneUser>() {
        override fun areItemsTheSame(oldItem: UserAndPhoneUser, newItem: UserAndPhoneUser) =
            oldItem.user.id == newItem.user.id

        override fun areContentsTheSame(oldItem: UserAndPhoneUser, newItem: UserAndPhoneUser) =
            oldItem == newItem
    }
}