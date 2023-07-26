package com.clover.studio.spikamessenger.ui.main.rooms.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.entity.MessageWithUser
import com.clover.studio.spikamessenger.databinding.ItemMessageSearchBinding
import com.clover.studio.spikamessenger.utils.Tools

class SearchAdapter(
    private val onItemClick: ((roomId: Int) -> Unit)
) : ListAdapter<MessageWithUser, SearchAdapter.SearchViewHolder>(SearchDiffCallback()) {
    inner class SearchViewHolder(val binding: ItemMessageSearchBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding =
            ItemMessageSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { item ->
                binding.tvUsername.text = item.user.displayName

                binding.tvMessageDate.text = item.message.createdAt?.let {
                    Tools.fullDateFormat(
                        it
                    )
                }
                binding.tvMessageContent.text = item.message.body?.text

                itemView.setOnClickListener {
                    item.let {
                        it.message.roomId?.let { roomId -> onItemClick.invoke(roomId) }
                    }
                }
            }
        }
    }


    private class SearchDiffCallback : DiffUtil.ItemCallback<MessageWithUser>() {

        override fun areItemsTheSame(
            oldItem: MessageWithUser,
            newItem: MessageWithUser
        ) =
            oldItem.message.id == newItem.message.id

        override fun areContentsTheSame(
            oldItem: MessageWithUser,
            newItem: MessageWithUser
        ) =
            oldItem == newItem
    }
}