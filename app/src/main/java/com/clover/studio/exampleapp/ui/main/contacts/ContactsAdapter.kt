package com.clover.studio.exampleapp.ui.main.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.data.models.UserAndPhoneUser
import com.clover.studio.exampleapp.databinding.ItemContactBinding

class ContactsAdapter(
    private val context: Context,
    private val onItemClick: ((item: UserAndPhoneUser) -> Unit)
) :
    ListAdapter<UserAndPhoneUser, ContactsAdapter.ContactsViewHolder>(ContactsDiffCallback()) {

    inner class ContactsViewHolder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val binding =
            ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { userItem ->
                binding.tvHeader.text = userItem.phoneUser.name.substring(0, 1)
                binding.tvUsername.text = userItem.phoneUser.name
                binding.tvTitle.text = userItem.user.telephoneNumber

                Glide.with(context).load(userItem.user.avatarUrl).into(binding.ivUserImage)

                // if not first item, check if item above has the same header
                if (position > 0 && getItem(position - 1).phoneUser.name.substring(0, 1)
                    == userItem.phoneUser.name.substring(0, 1)
                ) {
                    binding.tvHeader.visibility = View.GONE
                } else {
                    binding.tvHeader.visibility = View.VISIBLE
                }

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