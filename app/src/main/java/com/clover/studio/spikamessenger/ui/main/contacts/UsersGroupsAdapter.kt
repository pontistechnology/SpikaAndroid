package com.clover.studio.spikamessenger.ui.main.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import com.clover.studio.spikamessenger.databinding.ItemContactBinding
import com.clover.studio.spikamessenger.utils.Tools.getFilePathUrl

class UsersGroupsAdapter(
    private val context: Context,
    private val isGroupCreation: Boolean,
    private val userIdsInRoom: List<Int>?,
    private val isForward: Boolean,
    private val onItemClick: ((item: PrivateGroupChats) -> Unit)
) :
    ListAdapter<PrivateGroupChats, UsersGroupsAdapter.ContactsViewHolder>(ContactsDiffCallback()) {

    inner class ContactsViewHolder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        val binding =
            ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        with(holder) {
            // Transparent view should ignore all click events on cards.
            binding.transparentView.setOnClickListener { }

            getItem(position).let {
                // Header
                binding.tvHeader.text = if (it.phoneNumber != null) {
                    it.userName?.uppercase()?.substring(0, 1)
                } else {
                    it.roomName?.uppercase()?.substring(0, 1)
                }

                // Username
                binding.tvUsername.text = if (it.phoneNumber != null) {
                    it.userName ?: it.userPhoneName
                } else {
                    it.roomName.toString()
                }

                // Title
                binding.tvTitle.text = it.phoneNumber ?: ""

                // Avatar
                val avatar = if (it.phoneNumber != null){
                    R.drawable.img_user_avatar
                } else {
                    R.drawable.img_group_avatar
                }

                Glide.with(context).load(getFilePathUrl(it.avatarId))
                    .placeholder(avatar)
                    .error(avatar)
                    .into(binding.ivUserImage)

                // Selected chats
                if (isForward || isGroupCreation) {
                    val setCheck = if (it.phoneNumber != null) {
                        if (it.selected) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    } else {
                        if (it.selected) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                    binding.ivCheckedUser.visibility = setCheck
                } else {
                    binding.ivCheckedUser.visibility = View.GONE
                }

                if (it.phoneNumber != null) {
                    if (userIdsInRoom?.contains(it.userId) == true) {
                        binding.transparentView.visibility =
                            View.VISIBLE
                        it.selected = true
                    } else binding.transparentView.visibility = View.GONE
                }

                if (it.isForwarded) {
                    // Recent chats
                    if (position == 0) {
                        binding.tvHeader.text = context.getString(R.string.recent_chats)
                        binding.tvHeader.visibility = View.VISIBLE
                    } else {
                        binding.tvHeader.visibility = View.GONE
                    }
                } else {
                    if (position > 0) {
                        // Check if item above has the same header
                        val previousItem = if (it.phoneNumber != null) {
                            getItem(position - 1)?.userName?.lowercase()?.substring(0, 1)
                        } else {
                            getItem(position - 1)?.roomName?.lowercase()?.substring(0, 1)
                        }

                        val currentItem = if (it.phoneNumber != null) {
                            it.userName?.lowercase()?.substring(0, 1)
                        } else {
                            it.roomName?.lowercase()?.substring(0, 1)
                        }

                        if (previousItem == currentItem) {
                            binding.tvHeader.visibility = View.GONE
                        } else {
                            binding.tvHeader.visibility = View.VISIBLE
                        }
                    } else {
                        binding.tvHeader.visibility = View.VISIBLE
                    }
                }


                itemView.setOnClickListener { _ ->
                    onItemClick.invoke(it)
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
