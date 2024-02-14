package com.clover.studio.spikamessenger.ui.main.chat.media

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.databinding.ItemMediaSmallBinding
import com.clover.studio.spikamessenger.utils.Tools

class SmallMediaAdapter(
    private val context: Context,
    private val onItemClick: (message: Message) -> Unit
) : ListAdapter<Message, SmallMediaAdapter.SmallMediaViewHolder>(SmallMediaDiffCallback()) {

    private var selectedPosition = 0
    private var previousPosition = 0

    inner class SmallMediaViewHolder(val binding: ItemMediaSmallBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmallMediaViewHolder {
        val binding =
            ItemMediaSmallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SmallMediaViewHolder(binding)
    }

    fun setSelectedSmallMedia(index: Int) {
        previousPosition = selectedPosition
        selectedPosition = index

        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    override fun onBindViewHolder(holder: SmallMediaViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { message ->
                binding.vSelectedMedia.visibility = if (position == selectedPosition) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                val imagePath = Tools.getMediaFile(context, message)

                Glide.with(context)
                    .load(imagePath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            // Ignore
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }
                    })
                    .into(binding.ivSmallMedia)

                binding.ivSmallMedia.setOnClickListener { _ ->
                    onItemClick.invoke(message)
                }
            }
        }
    }

    private class SmallMediaDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }
    }
}
