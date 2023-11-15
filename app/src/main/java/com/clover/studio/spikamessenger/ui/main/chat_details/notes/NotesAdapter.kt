package com.clover.studio.spikamessenger.ui.main.chat_details.notes

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.entity.Note
import com.clover.studio.spikamessenger.databinding.NotesOptionBinding
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel


class NotesAdapter(
    private val context: Context,
    private val onNoteInteraction: ((note: Note) -> Unit)
) :
    ListAdapter<Note, NotesAdapter.NotesViewHolder>(NoteCallback()) {

    inner class NotesViewHolder(val binding: NotesOptionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val binding =
            NotesOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { noteItem ->
                if (currentList.size - 1 > 0) {
                    val cornerRadius = 32.0f
                    val shapeAppearanceModel = when (position) {
                        0 -> ShapeAppearanceModel.builder()
                            .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                            .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                            .setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
                            .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
                            .build()

                        currentList.size - 1 -> ShapeAppearanceModel.builder()
                            .setTopLeftCorner(CornerFamily.ROUNDED, 0f)
                            .setTopRightCorner(CornerFamily.ROUNDED, 0f)
                            .setBottomLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                            .setBottomRightCorner(CornerFamily.ROUNDED, cornerRadius)
                            .build()

                        else -> ShapeAppearanceModel.builder()
                            .setTopLeftCorner(CornerFamily.ROUNDED, 0f)
                            .setTopRightCorner(CornerFamily.ROUNDED, 0f)
                            .setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
                            .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
                            .build()
                    }
                    binding.cvNotesOption.shapeAppearanceModel = shapeAppearanceModel
                }

                binding.tvNotesOption.text = noteItem.title

                itemView.setOnClickListener {
                    noteItem.let {
                        onNoteInteraction.invoke(it)
                    }
                }

//                binding.tvOnlyItem.text = if (noteItem.modifiedAt != noteItem.createdAt) {
//                    context.getString(
//                        R.string.note_edited_at,
//                        Tools.fullDateFormat(noteItem.modifiedAt).toString()
//                    )
//                } else {
//                    context.getString(
//                        R.string.note_created_at,
//                        Tools.fullDateFormat(noteItem.createdAt).toString()
//                    )
//                }
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
