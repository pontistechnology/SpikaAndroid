package com.clover.studio.spikamessenger.ui.main.chat

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.databinding.VideoLayoutBinding
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.helpers.ChatAdapterHelper

class VideoLayout(context: Context) :
    ConstraintLayout(context) {

    private var bindingSetup: VideoLayoutBinding = VideoLayoutBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup
    private var listener: VideoLayoutListener? = null

    interface VideoLayoutListener {
        fun mediaNavigation()
    }

    fun setVideoLayoutListener(listener: VideoLayoutListener) {
        this.listener = listener
    }

     fun bindVideo(
        chatMessage: MessageAndRecords
    ) = with(binding) {
        if (chatMessage.message.body?.file?.metaData?.duration?.toLong() != null) {
            tvVideoDuration.text =
                Tools.convertDurationInSeconds(chatMessage.message.body.file?.metaData?.duration!!.toLong())
        } else {
            tvVideoDuration.text = context.getString(R.string.audio_duration)
        }

        val imageResized = Tools.resizeImage(
            chatMessage.message.body?.file?.metaData?.width,
            chatMessage.message.body?.file?.metaData?.height
        )

        val mediaPath = Tools.getMediaFile(context, chatMessage.message)
        ChatAdapterHelper.loadMedia(
            context = context,
            mediaPath = mediaPath,
            mediaImage = ivVideoThumbnail,
            loadingImage = ivVideoLoading,
            height = imageResized.second,
            width = imageResized.first,
            playButton = ivPlayButton
        )
        ivPlayButton.setOnClickListener {
            listener?.mediaNavigation()
        }
    }

}
