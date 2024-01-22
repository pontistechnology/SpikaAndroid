package com.clover.studio.spikamessenger.ui.main.chat.chat_layouts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.databinding.FileLayoutBinding
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.helpers.Resource

class FileLayout(context: Context) :
    ConstraintLayout(context) {

    private var bindingSetup: FileLayoutBinding = FileLayoutBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup
    private var listener: FileLayoutListener? = null

    interface FileLayoutListener {
        fun downloadFile()
        fun resendFile()
        fun cancelFileUpload()
    }

    fun setFileLayoutListener(listener: FileLayoutListener) {
        this.listener = listener
    }

    fun bindFile(chatMessage: MessageAndRecords, sender: Boolean) = with(binding) {
        addFiles(
            context = context,
            fileExtension = chatMessage.message.body?.file?.fileName?.substringAfterLast(".")
        )

        if (sender) {
            val fileBody = chatMessage.message.body?.file
            tvFileTitle.text = fileBody?.fileName
            tvFileSize.text = Tools.calculateFileSize(fileBody?.size ?: 0)

            if (chatMessage.message.id < 0) {
                if (Resource.Status.LOADING.toString() == chatMessage.message.messageStatus) {
                    ivDownloadFile.visibility = View.GONE
                    pbFile.apply {
                        visibility = View.VISIBLE
                        secondaryProgress = chatMessage.message.uploadProgress
                    }
                    ivCancelFile.apply {
                        visibility = View.VISIBLE
                        setOnClickListener { _ ->
                            listener?.cancelFileUpload()
                        }
                    }
                } else {
                    ivCancelFile.visibility = View.GONE
                    ivDownloadFile.visibility = View.GONE
                    ivUploadFailed.apply {
                        visibility = View.VISIBLE
                        listener?.resendFile()
                    }
                    pbFile.apply {
                        secondaryProgress = 0
                        visibility = View.GONE
                    }
                }
            } else {
                pbFile.visibility = View.GONE
                ivUploadFailed.visibility = View.GONE
                ivCancelFile.visibility = View.GONE
                clFileMessage.setBackgroundResource(R.drawable.bg_message_send)

                setUpFileLayout(chatMessage)
            }
        } else {
            setUpFileLayout(chatMessage)
        }
    }

    private fun setUpFileLayout(chatMessage: MessageAndRecords) = with(binding) {
        tvFileTitle.text = chatMessage.message.body?.file?.fileName
        tvFileSize.text = Tools.calculateFileSize(chatMessage.message.body?.file?.size!!)

        ivDownloadFile.apply {
            visibility = View.VISIBLE
            setOnClickListener { listener?.downloadFile() }
        }
    }

    private fun addFiles(context: Context, fileExtension: String?) = with(binding) {
        val drawableResId = when (fileExtension) {
            Const.FileExtensions.PDF -> R.drawable.img_pdf_black
            Const.FileExtensions.ZIP, Const.FileExtensions.RAR -> R.drawable.img_folder_zip
            Const.FileExtensions.MP3, Const.FileExtensions.WAW -> R.drawable.img_audio_file
            else -> R.drawable.img_file_black
        }

        binding.ivFileType.setImageDrawable(
            ResourcesCompat.getDrawable(
                context.resources,
                drawableResId,
                null
            )
        )
    }
}
