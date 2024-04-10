package com.clover.studio.spikamessenger.ui.main.chat.media_links_docs

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.databinding.MediaItemsBinding
import com.clover.studio.spikamessenger.utils.Tools

class MediaAdapter(
    private val context: Context,
    private val onItemClick: (message: Message) -> Unit
) : ListAdapter<Message, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {
    inner class MediaViewHolder(val binding: MediaItemsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding =
            MediaItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { message ->
                val imagePath = Tools.getMediaPath(context, message)
                Glide.with(context)
                    .load(imagePath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .error(AppCompatResources.getDrawable(context, R.drawable.img_media_placeholder_error))
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
                            binding.pbMediaLoading.visibility = View.GONE
                            binding.flLoadingScreen.visibility = View.GONE
                            binding.ivMediaItem.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .into(binding.ivMediaItem)

                binding.ivMediaItem.setOnClickListener { _ ->
                    onItemClick.invoke(message)
                }
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
