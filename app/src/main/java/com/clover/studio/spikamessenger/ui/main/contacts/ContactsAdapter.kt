package com.clover.studio.spikamessenger.ui.main.contacts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import com.clover.studio.spikamessenger.databinding.ItemContactBinding
import com.clover.studio.spikamessenger.utils.Tools.getFilePathUrl
import timber.log.Timber

class ContactsAdapter(
    private val context: Context,
    private val isGroupCreation: Boolean,
    private val userIdsInRoom: List<Int>?,
    private val isForward: Boolean,
    private val onItemClick: ((item: PrivateGroupChats) -> Unit)
) :
    ListAdapter<PrivateGroupChats, ContactsAdapter.ContactsViewHolder>(ContactsDiffCallback()) {

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

            getItem(position).let { userItem ->

                // TODO add condition if room is private of group
                // Show transparent view if userId is already in the room while adding users
                // This is only for adding users to already created room
                if (userIdsInRoom?.contains(userItem.id) == true) {
                    binding.transparentView.visibility =
                        View.VISIBLE
                    userItem.isSelected = true
                } else binding.transparentView.visibility = View.GONE

                if (isGroupCreation || isForward) {
                    binding.ivCheckedUser.visibility = if (userItem.isSelected) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                } else binding.ivCheckedUser.visibility = View.GONE

                binding.tvHeader.text = userItem?.name?.uppercase()?.substring(0, 1)
                    ?: userItem.formattedDisplayName?.uppercase()?.substring(0, 1)
                binding.tvUsername.text =
                    userItem?.name ?: userItem.formattedDisplayName
                binding.tvTitle.text = userItem.telephoneNumber

                if (userItem.avatarFileId != null && userItem.avatarFileId > 0L) {
                    Glide.with(context).load(userItem.avatarFileId.let { getFilePathUrl(it) })
                        .placeholder(R.drawable.img_user_avatar)
                        .into(binding.ivUserImage)
                } else binding.ivUserImage.setImageDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.img_user_avatar
                    )
                )

                // if not first item, check if item above has the same header
                if (!userItem.isForwarded) {
                    if (position > 0) {
                        val previousItem =
                            getItem(position - 1)?.name?.lowercase()?.substring(0, 1)
                                ?: getItem(position - 1).formattedDisplayName?.lowercase()
                                    ?.substring(0, 1)

                        val currentItem = userItem?.name?.lowercase()?.substring(0, 1)
                            ?: userItem.formattedDisplayName?.lowercase()?.substring(0, 1)

                        if (previousItem == currentItem) {
                            binding.tvHeader.visibility = View.GONE
                        } else {
                            binding.tvHeader.visibility = View.VISIBLE
                        }
                    } else {
                        binding.tvHeader.visibility = View.VISIBLE
                    }
                } else {
                    // Recent chats
                    Timber.d("Recent contact: $userItem!")
                    if (position == 0) {
                        binding.tvHeader.text = context.getString(R.string.recent_chats)
                        binding.tvHeader.visibility = View.VISIBLE
                    } else {
                        binding.tvHeader.visibility = View.GONE
                    }
                }

                itemView.setOnClickListener {
                    userItem.let {
                        onItemClick.invoke(it)
                    }
                }
            }
        }
    }

    private class ContactsDiffCallback : DiffUtil.ItemCallback<PrivateGroupChats>() {
        override fun areItemsTheSame(oldItem: PrivateGroupChats, newItem: PrivateGroupChats) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PrivateGroupChats, newItem: PrivateGroupChats) =
            oldItem == newItem
    }
}
