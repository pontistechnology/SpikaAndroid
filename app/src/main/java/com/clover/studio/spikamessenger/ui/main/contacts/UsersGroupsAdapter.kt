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

const val MAX_ALPHA = 1F
const val SELECTED_ALPHA = 0.4F

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
                binding.tvUsername.text = if (it.phoneNumber != null || it.isBot) {
                    it.userName ?: it.userPhoneName
                } else {
                    it.roomName.toString()
                }

                // Title
                binding.tvTitle.text = it.phoneNumber ?: ""

                // Avatar
                val avatar = if (it.phoneNumber != null || it.isBot) {
                    R.drawable.img_user_avatar
                } else {
                    R.drawable.img_group_avatar
                }

                Glide.with(context).load(getFilePathUrl(it.avatarId))
                    .placeholder(avatar)
                    .error(avatar)
                    .into(binding.ivUserImage)

                // Selected chats - set transparent view to already selected chats
                if (isForward || isGroupCreation) {
                    val isSelected = userIdsInRoom?.contains(it.userId) == true
                    binding.transparentView.visibility = if (isSelected) View.VISIBLE else View.GONE
                    val alphaValue = if (isSelected) SELECTED_ALPHA else MAX_ALPHA
                    with(binding) {
                        ivUserImage.alpha = alphaValue
                        tvUsername.alpha = alphaValue
                        tvTitle.alpha = alphaValue
                    }
                    binding.ivCheckedUser.visibility = if (it.selected) View.VISIBLE else View.GONE
                } else {
                    binding.ivCheckedUser.visibility = View.GONE
                }


                if (it.isRecent) {
                    // Recent chats
                    if (position == 0) {
                        binding.tvHeader.text = context.getString(R.string.recent_chats)
                        binding.tvHeader.visibility = View.VISIBLE
                    } else {
                        binding.tvHeader.visibility = View.GONE
                    }
                } else {
                    if (position > 0) {
                        val previousUser = getItem(position - 1)
                        val previousItem = if (it.phoneNumber != null) {
                            previousUser.userName?.lowercase()?.substring(0, 1)
                        } else {
                            previousUser.roomName?.lowercase()?.substring(0, 1)
                        }

                        val currentItem = if (it.phoneNumber != null) {
                            it.userName?.lowercase()?.substring(0, 1)
                        } else {
                            it.roomName?.lowercase()?.substring(0, 1)
                        }

                        if (previousItem == currentItem && !previousUser.isRecent) {
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
        override fun areItemsTheSame(
            oldItem: PrivateGroupChats,
            newItem: PrivateGroupChats
        ): Boolean {
            return if (oldItem.phoneNumber != null && newItem.phoneNumber != null) {
                oldItem.userId == newItem.userId
            } else {
                oldItem.roomId == newItem.roomId
            }
        }

        override fun areContentsTheSame(
            oldItem: PrivateGroupChats,
            newItem: PrivateGroupChats
        ): Boolean {
            return if (oldItem.phoneNumber != null && newItem.phoneNumber != null) {
                oldItem.userId == newItem.userId
            } else {
                oldItem.roomId == newItem.roomId
            }
        }
    }
}
