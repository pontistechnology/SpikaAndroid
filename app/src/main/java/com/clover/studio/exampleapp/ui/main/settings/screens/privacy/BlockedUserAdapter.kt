package com.clover.studio.exampleapp.ui.main.settings.screens.privacy

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.databinding.ItemBlockedUserBinding

class BlockedUserAdapter(
    private val context: Context,
    private val onItemClick: ((userId: Int) -> Unit)
) :
    ListAdapter<User, BlockedUserAdapter.BlockedUserViewHolder>(ContactsDiffCallback()) {

    inner class BlockedUserViewHolder(val binding: ItemBlockedUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedUserViewHolder {
        val binding =
            ItemBlockedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BlockedUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlockedUserViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { blockedUser ->
                // TODO fill out adapter
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