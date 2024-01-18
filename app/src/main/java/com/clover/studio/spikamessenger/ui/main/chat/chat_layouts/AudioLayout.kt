package com.clover.studio.spikamessenger.ui.main.chat.chat_layouts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.databinding.AudioLayoutBinding
import com.clover.studio.spikamessenger.utils.helpers.Resource

class AudioLayout(context: Context) :
    ConstraintLayout(context) {

    private var bindingSetup: AudioLayoutBinding = AudioLayoutBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup
    private var listener: AudioLayoutListener? = null

    interface AudioLayoutListener {
        fun audioPlayClicked()
        fun audioSeekBarPressed(progress: Int)
        fun audioResend()
        fun audioCancelUpload()
    }

    fun setupAudioLayoutListener(listener: AudioLayoutListener) {
        this.listener = listener
    }

    fun bindAudio(chatMessage: MessageAndRecords) = with(binding) {
        if (chatMessage.message.id < 0) {
            if (Resource.Status.LOADING.toString() == chatMessage.message.messageStatus) {
                ivPlayAudio.visibility = View.GONE
                pbAudio.apply {
                    visibility = View.VISIBLE
                    secondaryProgress = chatMessage.message.uploadProgress
                }
                ivCancelAudio.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { _ ->
                        listener?.audioCancelUpload()
                    }
                }
            } else {
                ivCancelAudio.visibility = View.GONE
                ivPlayAudio.visibility = View.GONE
                pbAudio.apply {
                    visibility = View.GONE
                    secondaryProgress = 0
                }
                ivUploadFailed.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { _ ->
                        listener?.audioResend()
                    }
                }
            }
        } else {
            ivPlayAudio.visibility = View.VISIBLE

            ivPlayAudio.setOnClickListener {
                listener?.audioPlayClicked()
            }

            sbAudio.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        listener?.audioSeekBarPressed(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // Ignore
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // Ignore
                }
            })
        }
    }

    fun setProgress(progress: Int) = with(binding) {
        sbAudio.progress = progress
    }

    fun setMaxProgress(maxProgress: Int) = with(binding) {
        sbAudio.max = maxProgress
    }

    fun setDuration(duration: String) = with(binding) {
        tvAudioDuration.text = duration
    }

    fun setPlayImage(drawable: Int) = with(binding) {
        ivPlayAudio.setImageResource(drawable)
    }

    fun setPlayVisibility(visibility: Int) = with(binding) {
        ivPlayAudio.visibility = visibility
    }
}
