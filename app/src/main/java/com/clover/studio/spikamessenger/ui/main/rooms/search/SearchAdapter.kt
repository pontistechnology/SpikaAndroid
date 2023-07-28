package com.clover.studio.spikamessenger.ui.main.rooms.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.entity.MessageWithRoom
import com.clover.studio.spikamessenger.databinding.ItemMessageSearchBinding
import com.clover.studio.spikamessenger.utils.Tools

class SearchAdapter(
    private val onItemClick: ((messageWithRoom: MessageWithRoom) -> Unit)
) : ListAdapter<MessageWithRoom, SearchAdapter.SearchViewHolder>(SearchDiffCallback()) {
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
                for (user in item.roomWithUsers?.users!!) {
                    if (user.id == item.message.fromUserId) {
                        binding.tvUsername.text = user.displayName
                        break
                    }
                }

                binding.tvMessageDate.text = item.message.createdAt?.let {
                    Tools.fullDateFormat(
                        it
                    )
                }
                binding.tvMessageContent.text = item.message.body?.text

                binding.tvHeader.text = item.roomWithUsers.room.name

                // if not first item, check if item above has the same header
                if (position > 0) {
                    val previousItem =
                        getItem(position - 1).roomWithUsers?.room?.name?.lowercase()
                            ?.substring(0, 1)

                    val currentItem = item.roomWithUsers.room.name?.lowercase()?.substring(0, 1)

                    if (previousItem == currentItem) {
                        binding.tvHeader.visibility = View.GONE
                    } else {
                        binding.tvHeader.visibility = View.VISIBLE
                    }
                } else {
                    binding.tvHeader.visibility = View.VISIBLE
                }

                itemView.setOnClickListener {
                    item.let {
                        onItemClick.invoke(it)
                    }
                }
            }
        }
    }


    private class SearchDiffCallback : DiffUtil.ItemCallback<MessageWithRoom>() {

        override fun areItemsTheSame(
            oldItem: MessageWithRoom,
            newItem: MessageWithRoom
        ) =
            oldItem.message.id == newItem.message.id

        override fun areContentsTheSame(
            oldItem: MessageWithRoom,
            newItem: MessageWithRoom
        ) =
            oldItem == newItem
    }
}