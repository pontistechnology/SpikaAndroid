package com.clover.studio.exampleapp.ui.main.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.ItemContactBinding

class ContactsAdapter(
    private val context: Context,
    private val onItemClick: ((item: User) -> Unit)
) :
    ListAdapter<User, ContactsAdapter.ContactsViewHolder>(ContactsDiffCallback()) {

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
                binding.tvHeader.text = userItem.displayName?.substring(0, 1)
                binding.tvUsername.text = userItem.displayName
                // TODO add user title

                Glide.with(context).load(userItem.avatarUrl).into(binding.ivUserImage)

                // if not first item, check if item above has the same header
                if (position > 0 && getItem(position - 1).displayName?.substring(
                        0,
                        1
                    ) == userItem.displayName?.substring(0, 1)
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

    private class ContactsDiffCallback : DiffUtil.ItemCallback<User>() {

        override fun areItemsTheSame(oldItem: User, newItem: User) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: User, newItem: User) =
            oldItem == newItem
    }
}