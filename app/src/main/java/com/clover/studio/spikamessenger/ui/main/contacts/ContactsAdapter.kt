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

            getItem(position).let {

                if (it.private != null) {
                    // Private room
                    if (userIdsInRoom?.contains(it.private.user.id) == true) {
                        binding.transparentView.visibility =
                            View.VISIBLE
                        // TODO
                        it.private.user.selected = true
                    } else binding.transparentView.visibility = View.GONE

                    if (isGroupCreation || isForward) {
                        binding.ivCheckedUser.visibility = if (it.private.user.selected) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    } else binding.ivCheckedUser.visibility = View.GONE

                    binding.tvHeader.text = it.private.phoneUser?.name?.uppercase()?.substring(0, 1)
                        ?: it.private.user.formattedDisplayName.uppercase().substring(0, 1)
                    binding.tvUsername.text =
                        it.private.phoneUser?.name ?: it.private.user.formattedDisplayName
                    binding.tvTitle.text = it.private.user.telephoneNumber

                    if (it.private.user.hasAvatar) {
                        Glide.with(context).load(it.private.user.avatarFileId?.let { avatar ->
                            getFilePathUrl(avatar)
                        })
                            .placeholder(R.drawable.img_user_avatar)
                            .into(binding.ivUserImage)
                    } else binding.ivUserImage.setImageDrawable(
                        AppCompatResources.getDrawable(
                            context,
                            R.drawable.img_user_avatar
                        )
                    )
                    if (!it.private.user.isForwarded) {
                        if (position > 0) {
                            val previousItem =
                                getItem(position - 1)?.private?.phoneUser?.name?.lowercase()
                                    ?.substring(0, 1)
                                    ?: getItem(position - 1).private?.user?.formattedDisplayName?.lowercase()
                                        ?.substring(0, 1)

                            val currentItem =
                                it.private.phoneUser?.name?.lowercase()?.substring(0, 1)
                                    ?: it.private.user.formattedDisplayName.lowercase()
                                        .substring(0, 1)

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
//                        Timber.d("Recent contact: $userItem!")
                        if (position == 0) {
                            binding.tvHeader.text = context.getString(R.string.recent_chats)
                            binding.tvHeader.visibility = View.VISIBLE
                        } else {
                            binding.tvHeader.visibility = View.GONE
                        }
                    }

                    itemView.setOnClickListener { _ ->
                        onItemClick.invoke(it)
                    }

                } else {
                    // Group room
                }


                // if not first item, check if item above has the same header

            }
        }
    }

    private class ContactsDiffCallback : DiffUtil.ItemCallback<PrivateGroupChats>() {
        // TODO
        override fun areItemsTheSame(oldItem: PrivateGroupChats, newItem: PrivateGroupChats) =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: PrivateGroupChats, newItem: PrivateGroupChats) =
            oldItem == newItem
    }
}
