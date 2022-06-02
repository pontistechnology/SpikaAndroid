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
import com.clover.studio.exampleapp.utils.Tools.getAvatarUrl
import timber.log.Timber

class ContactsAdapter(
    private val context: Context,
    private val isGroupCreation: Boolean,
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
                if (isGroupCreation) {
                    binding.cbUserSelected.visibility = View.VISIBLE

                    binding.cbUserSelected.isChecked = userItem.user.selected
                }

                binding.tvHeader.text = userItem.phoneUser?.name?.uppercase()?.substring(0, 1)
                    ?: userItem.user.displayName?.uppercase()?.substring(0, 1)
                binding.tvUsername.text = userItem.phoneUser?.name ?: userItem.user.displayName
                binding.tvTitle.text = userItem.user.telephoneNumber

                // Remove first / with substring from avatarUrl
                Glide.with(context).load(userItem.user.avatarUrl?.let { getAvatarUrl(it) })
                    .into(binding.ivUserImage)

                // if not first item, check if item above has the same header
                if (position > 0) {
                    val previousItem =
                        getItem(position - 1).phoneUser?.name?.lowercase()?.substring(0, 1)
                            ?: getItem(position - 1).user.displayName?.lowercase()?.substring(0, 1)

                    val currentItem = userItem.phoneUser?.name?.lowercase()?.substring(0, 1)
                        ?: userItem.user.displayName?.lowercase()?.substring(0, 1)
                    Timber.d("Items : $previousItem, $currentItem ${previousItem == currentItem}")

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