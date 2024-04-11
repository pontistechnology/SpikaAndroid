package com.clover.studio.spikamessenger.ui.main.chat.media_links_docs

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.databinding.MediaItemsBinding
import com.clover.studio.spikamessenger.databinding.MonthDateBinding
import com.clover.studio.spikamessenger.utils.Tools

const val VIEW_TYPE_MEDIA_ITEM = 1
const val VIEW_TYPE_DATE_ITEM = 2

class MediaAdapter(
    private val context: Context,
    private val onItemClick: (message: Message) -> Unit
) : ListAdapter<Message, ViewHolder>(MediaDiffCallback()) {
    inner class MediaViewHolder(val binding: MediaItemsBinding) :
        ViewHolder(binding.root)

    inner class MonthDateViewHolder(val binding: MonthDateBinding) :
        ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == VIEW_TYPE_MEDIA_ITEM) {
            val binding =
                MediaItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MediaViewHolder(binding)
        } else {
            val binding =
                MonthDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MonthDateViewHolder(binding)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.roomId == null) {
            VIEW_TYPE_DATE_ITEM
        } else {
            VIEW_TYPE_MEDIA_ITEM
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position).let { message ->
            if (holder.itemViewType == VIEW_TYPE_MEDIA_ITEM) {
                holder as MediaViewHolder

                val imagePath = Tools.getMediaPath(context, message)
                Glide.with(context)
                    .load(imagePath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .error(
                        AppCompatResources.getDrawable(
                            context,
                            R.drawable.img_media_placeholder_error
                        )
                    )
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            holder.binding.pbMediaLoading.visibility = View.GONE
                            holder.binding.flLoadingScreen.visibility = View.GONE
                            holder.binding.ivMediaItem.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .into(holder.binding.ivMediaItem)

                holder.binding.ivMediaItem.setOnClickListener { _ ->
                    onItemClick.invoke(message)
                }

            } else {
                holder as MonthDateViewHolder
                holder.binding.tvTitle.text = message.body?.text.toString()
            }
        }
    }

    private class MediaDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }
    }
}
