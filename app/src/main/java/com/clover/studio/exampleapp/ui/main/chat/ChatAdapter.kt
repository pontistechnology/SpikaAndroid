package com.clover.studio.exampleapp.ui.main.chat

import android.content.Context
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.ItemMessageMeBinding
import com.clover.studio.exampleapp.databinding.ItemMessageOtherBinding
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.Tools.getRelativeTimeSpan
import timber.log.Timber
import java.util.*

private const val VIEW_TYPE_MESSAGE_SENT = 1
private const val VIEW_TYPE_MESSAGE_RECEIVED = 2

class ChatAdapter(
    private val context: Context,
    private val myUserId: Int,
    private val users: List<User>
) :
    ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    inner class SentMessageHolder(val binding: ItemMessageMeBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class ReceivedMessageHolder(val binding: ItemMessageOtherBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            val binding =
                ItemMessageMeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentMessageHolder(binding)
        } else {
            val binding =
                ItemMessageOtherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedMessageHolder(binding)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        return if (message.fromUserId == myUserId) {
            VIEW_TYPE_MESSAGE_SENT
        } else {
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position).let {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it.createdAt!!
            val date = calendar.get(Calendar.DAY_OF_MONTH)

            if (holder.itemViewType == VIEW_TYPE_MESSAGE_SENT) {
                (holder as SentMessageHolder).binding.tvMessage.text = it.body?.text

                if (it.body?.text.isNullOrEmpty()) {
                    holder.binding.tvMessage.visibility = View.GONE
                    holder.binding.ivChatImage.visibility = View.VISIBLE

                    Glide.with(context)
                        .load(it.body?.file?.path?.let { imagePath -> Tools.getAvatarUrl(imagePath) })
                        .into(holder.binding.ivChatImage)
                } else {
                    holder.binding.tvMessage.visibility = View.VISIBLE
                    holder.binding.ivChatImage.visibility = View.GONE
                }

                showDateHeader(position, date, holder.binding.tvSectionHeader, it)

                when {
                    it.seenCount!! > 0 -> {
                        holder.binding.ivMessageStatus.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.img_seen
                            )
                        )
                    }
                    it.totalUserCount == it.deliveredCount -> {
                        holder.binding.ivMessageStatus.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.img_done
                            )
                        )
                    }
                    it.deliveredCount!! >= 0 -> {
                        holder.binding.ivMessageStatus.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.img_sent
                            )
                        )
                    }
                    else -> {
                        holder.binding.ivMessageStatus.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.img_clock
                            )
                        )
                    }
                }
            } else {
                (holder as ReceivedMessageHolder).binding.tvMessage.text = it.body?.text
                for (roomUser in users) {
                    if (it.fromUserId == roomUser.id) {
                        holder.binding.tvUsername.text = roomUser.displayName
                        Glide.with(context)
                            .load(roomUser.avatarUrl?.let { avatarUrl ->
                                Tools.getAvatarUrl(
                                    avatarUrl
                                )
                            })
                            .into(holder.binding.ivUserImage)
                        break
                    }
                }

                showDateHeader(position, date, holder.binding.tvSectionHeader, it)

                if (position > 0) {
                    try {
                        val nextItem = getItem(position + 1).fromUserId

                        val currentItem = it.fromUserId
                        Timber.d("Items : $nextItem, $currentItem ${nextItem == currentItem}")

                        if (nextItem == currentItem) {
                            holder.binding.tvUsername.visibility = View.GONE
                        } else {
                            holder.binding.tvUsername.visibility = View.VISIBLE
                        }
                    } catch (ex: IndexOutOfBoundsException) {
                        Tools.checkError(ex)
                        holder.binding.tvUsername.visibility = View.VISIBLE
                    }
                } else {
                    holder.binding.tvUsername.visibility = View.VISIBLE
                }
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {

        override fun areItemsTheSame(oldItem: Message, newItem: Message) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Message, newItem: Message) =
            oldItem == newItem
    }

    private fun showDateHeader(
        position: Int,
        date: Int,
        view: TextView,
        message: Message
    ) {
        if (position >= 0 && currentList.size - 1 > position) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = getItem(position + 1).createdAt!!
            val previousDate = calendar.get(Calendar.DAY_OF_MONTH)

            if (date != previousDate) {
                view.visibility = View.VISIBLE
            } else view.visibility = View.GONE

            view.text = message.createdAt?.let {
                DateUtils.getRelativeTimeSpanString(
                    it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS
                )
            }
        } else {
            view.visibility = View.VISIBLE
            view.text =
                message.createdAt?.let { getRelativeTimeSpan(it) }
        }
    }
}