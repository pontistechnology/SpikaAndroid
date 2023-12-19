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
import timber.log.Timber

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
                binding.tvHeader.text = if (it.private != null) {
                    it.private.phoneUser?.name?.uppercase()?.substring(0, 1)
                        ?: it.private.user.formattedDisplayName.uppercase().substring(0, 1)
                } else {
                    it.group!!.room.name?.uppercase()?.substring(0, 1)
                }

                // Username
                binding.tvUsername.text = if (it.private != null) {
                    it.private.phoneUser?.name ?: it.private.user.formattedDisplayName
                } else {
                    it.group!!.room.name.toString()
                }

                // Title
                binding.tvTitle.text = if (it.private != null) {
                    it.private.user.telephoneNumber
                } else {
                    it.group!!.room.name.toString()
                }

                // Avatar
                val avatarId = if (it.private != null) {
                    it.private.user.avatarFileId ?: 0L
                } else {
                    it.group!!.room.avatarFileId ?: 0L
                }

                Glide.with(context).load(getFilePathUrl(avatarId))
                    .placeholder(R.drawable.img_user_avatar)
                    .error(R.drawable.img_user_avatar)
                    .into(binding.ivUserImage)

                if (isForward || isGroupCreation) {
                    val setCheck = if (it.private != null) {
                        if (it.private.user.selected) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    } else {
                        if (it.group!!.room.selected) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                    binding.ivCheckedUser.visibility = setCheck
                } else {
                    binding.ivCheckedUser.visibility = View.GONE
                }

                if (it.private != null) {
                    if (userIdsInRoom?.contains(it.private.user.id) == true) {
                        binding.transparentView.visibility =
                            View.VISIBLE
                        it.private.user.selected = true
                    } else binding.transparentView.visibility = View.GONE
                }

                if ((it.private != null && it.private.user.isForwarded) || (it.group != null && it.group.room.isForwarded)) {
                    // Logic for items that are forwarded
                    Timber.d("Here, $it")
                    // Recent chats
                    if (position == 0) {
                        binding.tvHeader.text = context.getString(R.string.recent_chats)
                        binding.tvHeader.visibility = View.VISIBLE
                    } else {
                        binding.tvHeader.visibility = View.GONE
                    }
                } else {
                    // Logic for items that are not forwarded
                    Timber.d("Here, other logic")
                    if (position > 0) {
                        // Check if item above has the same header
                        val previousItem = if (it.private != null) {
                            getItem(position - 1)?.private?.phoneUser?.name?.lowercase()
                                ?.substring(0, 1)
                                ?: getItem(position - 1).private?.user?.formattedDisplayName?.lowercase()
                                    ?.substring(0, 1)
                        } else {
                            getItem(position - 1)?.group!!.room.name?.lowercase()?.substring(0, 1)
                        }

                        val currentItem = if (it.private != null) {
                            it.private.phoneUser?.name?.lowercase()?.substring(0, 1)
                                ?: it.private.user.formattedDisplayName.lowercase()
                                    .substring(0, 1)
                        } else {
                            it.group!!.room.name?.lowercase()?.substring(0, 1)
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
