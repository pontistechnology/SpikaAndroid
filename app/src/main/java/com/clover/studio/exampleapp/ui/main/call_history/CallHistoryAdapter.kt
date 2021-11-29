package com.clover.studio.exampleapp.ui.main.call_history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.ItemCallHistoryBinding

class CallHistoryAdapter(
    private val context: Context,
    private val onItemClick: ((item: User) -> Unit)
) :
    ListAdapter<User, CallHistoryAdapter.CallHistoryViewHolder>(HistoryDiffCallback()) {

    inner class CallHistoryViewHolder(val binding: ItemCallHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallHistoryViewHolder {
        val binding =
            ItemCallHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CallHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallHistoryViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { userItem ->
                binding.tvUsername.text = userItem.displayName
//                binding.tvCallDirection = // TODO set direction text
//                binding.ivCallIcon.setImageDrawable() // TODO set image for call icon
//                binding.tvCallTime.text = // TODO set call time
//                binding.ivCallType.setImageDrawable() // TODO set image for call type

                Glide.with(context).load(userItem.avatarUrl).into(binding.ivPickPhoto)

                itemView.setOnClickListener {
                    userItem.let {
                        onItemClick.invoke(it)
                    }
                }
            }
        }
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<User>() {

        override fun areItemsTheSame(oldItem: User, newItem: User) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: User, newItem: User) =
            oldItem == newItem
    }
}