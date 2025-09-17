package com.clover.studio.spikamessenger.ui.main.call_history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.databinding.ItemCallHistoryBinding
import com.clover.studio.spikamessenger.utils.Tools.getFilePathUrl

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
                binding.tvUsername.text = userItem.formattedDisplayName

                Glide.with(context).load(userItem.avatarFileId?.let { getFilePathUrl(it) })
                    .into(binding.ivPickPhoto)

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
