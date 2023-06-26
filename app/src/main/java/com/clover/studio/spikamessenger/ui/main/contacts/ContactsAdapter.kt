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
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.databinding.ItemContactBinding
import com.clover.studio.spikamessenger.utils.Tools.getFilePathUrl

class ContactsAdapter(
    private val context: Context,
    private val isGroupCreation: Boolean,
    private val userIdsInRoom: List<Int>?,
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
            // Transparent view should ignore all click events on cards.
            binding.transparentView.setOnClickListener { }

            getItem(position).let { userItem ->
                // Show transparent view if userId is already in the room while adding users
                // This is only for adding users to already created room
                if (userIdsInRoom?.contains(userItem.user.id) == true) {
                    binding.transparentView.visibility =
                        View.VISIBLE
                    userItem.user.selected = true
                } else binding.transparentView.visibility = View.GONE


                if (isGroupCreation) {
                    binding.cbUserSelected.visibility = View.VISIBLE

                    binding.cbUserSelected.isChecked = userItem.user.selected
                } else binding.cbUserSelected.visibility = View.GONE

                binding.tvHeader.text = userItem.phoneUser?.name?.uppercase()?.substring(0, 1)
                    ?: userItem.user.formattedDisplayName?.uppercase()?.substring(0, 1)
                binding.tvUsername.text =
                    userItem.phoneUser?.name ?: userItem.user.formattedDisplayName
                binding.tvTitle.text = userItem.user.telephoneNumber

                if (userItem.user.hasAvatar) {
                    Glide.with(context).load(userItem.user.avatarFileId?.let { getFilePathUrl(it) })
                        .placeholder(R.drawable.img_user_placeholder)
                        .centerCrop()
                        .into(binding.ivUserImage)
                } else binding.ivUserImage.setImageDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        R.drawable.img_user_placeholder
                    )
                )

                // if not first item, check if item above has the same header
                if (position > 0) {
                    val previousItem =
                        getItem(position - 1).phoneUser?.name?.lowercase()?.substring(0, 1)
                            ?: getItem(position - 1).user.formattedDisplayName?.lowercase()
                                ?.substring(0, 1)

                    val currentItem = userItem.phoneUser?.name?.lowercase()?.substring(0, 1)
                        ?: userItem.user.formattedDisplayName?.lowercase()?.substring(0, 1)

                    if (previousItem == currentItem) {
                        binding.tvHeader.visibility = View.GONE
                    } else {
                        binding.tvHeader.visibility = View.VISIBLE
                    }
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