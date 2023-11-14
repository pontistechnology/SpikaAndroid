package com.clover.studio.spikamessenger.ui.main.chat_details.notes

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.clover.studio.spikamessenger.data.models.entity.Note
import com.clover.studio.spikamessenger.databinding.FirstChatOptionBinding
import com.clover.studio.spikamessenger.databinding.OnlyChatOptionBinding
import timber.log.Timber

const val VIEW_TYPE_ONLY_NOTE = 0
const val VIEW_TYPE_FIRST_NOTE = 1
const val VIEW_TYPE_NOTE = 2
// TODO last


class NotesAdapter(
    private val context: Context,
    private val onNoteInteraction: ((note: Note) -> Unit)
) :
    ListAdapter<Note, ViewHolder>(NoteCallback()) {

    inner class OnlyNoteViewHolder(val binding: OnlyChatOptionBinding) :
        ViewHolder(binding.root)

    inner class FirstNoteViewHolder(val binding: FirstChatOptionBinding) :
        ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == VIEW_TYPE_ONLY_NOTE) {
            val binding =
                OnlyChatOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            OnlyNoteViewHolder(binding)
        } else {
            val binding =
                FirstChatOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            FirstNoteViewHolder(binding)
        }
    }

    override fun getItemViewType(position: Int): Int {
        Timber.d("position: $position, ${currentList.size}")
        if (currentList.size == 1) {
            return VIEW_TYPE_ONLY_NOTE
        }
//        if (position == currentList.size -1){
//            return
//        }
//        if (position == 0 && currentList.size > 1){
//
//        }
        else {
            return VIEW_TYPE_FIRST_NOTE
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position).let { noteItem ->
            if (holder.itemViewType == VIEW_TYPE_ONLY_NOTE) {
                holder as OnlyNoteViewHolder
                holder.binding.tvOnlyItem.text = noteItem.title

                holder.itemView.setOnClickListener {
                    noteItem.let {
                        onNoteInteraction.invoke(it)
                    }
                }
            } else {
                holder as FirstNoteViewHolder

                holder.binding.tvFirstItem.text = noteItem.title

                holder.itemView.setOnClickListener {
                    noteItem.let {
                        onNoteInteraction.invoke(it)
                    }
                }

            }
        }

    }

    private class NoteCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Note, newItem: Note) =
            oldItem == newItem
    }
}
