package com.clover.studio.spikamessenger.ui.main.chat

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.databinding.ItemMediaBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import timber.log.Timber

class MediaPagerAdapter(private val context: Context,
                        private val mediaList: List<Message>,
                        private val onItemClicked: (event: String, message: Message) -> Unit) :
    RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return MediaViewHolder(binding)
    }

    override fun getItemCount(): Int = mediaList.size

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        bindMediaImage(holder.binding, position)
    }


    private fun bindMediaImage(binding: ItemMediaBinding, position: Int) {
        binding.clImageContainer.visibility = View.VISIBLE
        binding.ivFullImage.setOnClickListener { _ ->
            onItemClicked.invoke(Const.MediaActions.MEDIA_SHOW_BARS, mediaList[position])
        }

        val imagePath = mediaList[position].body?.fileId?.let {
            Tools.getFilePathUrl(it)
        }.toString()

        Timber.d("HERE, position: $position")
        Timber.d("HERE, imagePath: $imagePath")

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
                    binding.pbMediaImage.visibility = View.GONE
                    return false
                }
            })
            .into(binding.ivFullImage)
    }
}

