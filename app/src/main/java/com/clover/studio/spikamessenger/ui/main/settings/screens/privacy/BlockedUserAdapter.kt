package com.clover.studio.spikamessenger.ui.main.settings.screens.privacy

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.databinding.ItemBlockedUserBinding
import com.clover.studio.spikamessenger.utils.Tools

class BlockedUserAdapter(
    private val context: Context,
    private val onItemClick: ((user: User) -> Unit)
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
                binding.tvUserName.text = blockedUser.formattedDisplayName
                binding.tvUserNumber.text = blockedUser.telephoneNumber

                Glide.with(context).load(blockedUser.avatarFileId?.let { Tools.getFilePathUrl(it) })
                    .placeholder(R.drawable.img_user_placeholder)
                    .centerCrop()
                    .into(binding.ivRoomImage)

                itemView.setOnClickListener {
                    blockedUser.let {
                        onItemClick.invoke(blockedUser)
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