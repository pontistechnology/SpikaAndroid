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
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.FileItemBinding
import com.clover.studio.spikamessenger.databinding.LinkItemBinding
import com.clover.studio.spikamessenger.databinding.MediaItemsBinding
import com.clover.studio.spikamessenger.databinding.MonthDateBinding
import com.clover.studio.spikamessenger.ui.main.chat.MediaType
import com.clover.studio.spikamessenger.utils.Tools

const val VIEW_TYPE_MEDIA_ITEM = 1
const val VIEW_TYPE_DATE_ITEM = 2
const val VIEW_TYPE_LINK_ITEM = 3
const val VIEW_TYPE_FILE_ITEM = 4

class MediaAdapter(
    private val context: Context,
    private val mediaType: MediaType,
    private val roomWithUsers: RoomWithUsers?,
    private val onItemClick: (message: Message) -> Unit
) : ListAdapter<Message, ViewHolder>(MediaDiffCallback()) {
    inner class MediaViewHolder(val binding: MediaItemsBinding) :
        ViewHolder(binding.root)

    inner class MonthDateViewHolder(val binding: MonthDateBinding) :
        ViewHolder(binding.root)

    inner class LinksViewHolder(val binding: LinkItemBinding) :
        ViewHolder(binding.root)

    inner class FilesViewHolder(val binding: FileItemBinding) :
        ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MEDIA_ITEM -> {
                val binding =
                    MediaItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MediaViewHolder(binding)
            }

            VIEW_TYPE_DATE_ITEM -> {
                val binding =
                    MonthDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MonthDateViewHolder(binding)
            }

            VIEW_TYPE_LINK_ITEM -> {
                val binding =
                    LinkItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                LinksViewHolder(binding)
            }

            VIEW_TYPE_FILE_ITEM -> {
                val binding =
                    FileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                FilesViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.roomId == null) {
            VIEW_TYPE_DATE_ITEM
        } else {
            when (mediaType) {
                MediaType.MEDIA -> VIEW_TYPE_MEDIA_ITEM
                MediaType.LINKS -> VIEW_TYPE_LINK_ITEM
                MediaType.FILES -> VIEW_TYPE_FILE_ITEM
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position).let { message ->
            when (holder.itemViewType) {
                VIEW_TYPE_DATE_ITEM -> bindDateItem(holder as MonthDateViewHolder, message)
                VIEW_TYPE_MEDIA_ITEM -> bindMediaItem(holder as MediaViewHolder, message)
                VIEW_TYPE_LINK_ITEM -> bindLinkItem(holder as LinksViewHolder, message)
                VIEW_TYPE_FILE_ITEM -> bindFileItem(holder as FilesViewHolder, message)
            }
        }
    }

    private fun bindFileItem(holder: FilesViewHolder, message: Message) {
        with(holder.binding) {
            tvFileTitle.text = message.body?.file?.fileName
            tvFileSize.text = Tools.calculateFileSize(message.body?.file?.size ?: 0)
            tvSender.text = roomWithUsers?.users?.find {
                it.id == message.fromUserId
            }?.displayName.toString()

            ivDownloadFile.setOnClickListener {

            }
        }
    }

    private fun bindDateItem(holder: MonthDateViewHolder, message: Message) {
        holder.binding.tvTitle.text = message.body?.text.toString()
    }

    private fun bindLinkItem(holder: LinksViewHolder, message: Message) {
        with(holder.binding) {
            if (message.body?.thumbnailData?.image?.isNotEmpty() == true) {
                Glide.with(context)
                    .load(message.body.thumbnailData?.image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.img_image_placeholder)
                    .into(holder.binding.ivPreviewImage)
                ivPreviewImage.visibility = View.VISIBLE
            }

            tvTitle.text = message.body?.thumbnailData?.title
            tvDescription.text = message.body?.thumbnailData?.description
                ?: message.body?.thumbnailData?.url
            tvLink.text = message.body?.text
            tvUsername.text = roomWithUsers?.users?.find {
                it.id == message.fromUserId
            }?.displayName.toString()

            llLinkItem.setOnClickListener {
                onItemClick.invoke(message)
            }
        }
    }

    private fun bindMediaItem(holder: MediaViewHolder, message: Message) {
        with(holder.binding) {
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
                        pbMediaLoading.visibility = View.GONE
                        flLoadingScreen.visibility = View.GONE
                        ivMediaItem.visibility = View.VISIBLE
                        return false
                    }
                })
                .into(ivMediaItem)

            ivMediaItem.setOnClickListener { _ ->
                onItemClick.invoke(message)
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
