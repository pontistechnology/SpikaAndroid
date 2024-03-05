package com.clover.studio.spikamessenger.ui.main.chat.chat_layouts

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.databinding.PreviewLayoutBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel

class PreviewLayout(context: Context) : ConstraintLayout(context) {
    private var bindingSetup: PreviewLayoutBinding =
        PreviewLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    private val binding get() = bindingSetup
    private var listener: PreviewLayoutListener? = null

    interface PreviewLayoutListener {
        fun previewLayoutClicked()
    }

    fun setPreviewLayoutListener(listener: PreviewLayoutListener) {
        this.listener = listener
    }

    fun bindPreview(
        context: Context,
        chatMessage: MessageAndRecords,
        clContainer: ConstraintLayout,
        sender: Boolean
    ) = with(binding) {

        clPreview.setOnClickListener {
            listener?.previewLayoutClicked()
        }

        if (chatMessage.message.body?.thumbnailData?.icon?.isNotEmpty() == true) {
            Glide.with(context)
                .load(chatMessage.message.body.thumbnailData?.image)
                .into(ivPreviewImage)
            setAppearanceModel(ivPreviewImage, sender)
        }

        tvTitle.text = chatMessage.message.body?.thumbnailData?.title
        tvDescription.text = chatMessage.message.body?.thumbnailData?.description
            ?: chatMessage.message.body?.thumbnailData?.url
    }

    private fun setAppearanceModel(ivPreviewImage: ShapeableImageView, sender: Boolean) {
        val builder = ShapeAppearanceModel().toBuilder()

        if (sender) {
            builder.setTopRightCorner(
                CornerFamily.ROUNDED,
                resources.getDimension(R.dimen.eight_dp_margin)
            )
                .setTopLeftCorner(
                    CornerFamily.ROUNDED,
                    resources.getDimension(R.dimen.eight_dp_margin)
                )
        } else {
            builder.setTopRightCorner(
                CornerFamily.ROUNDED,
                resources.getDimension(R.dimen.eight_dp_margin)
            )
                .setBottomRightCorner(
                    CornerFamily.ROUNDED,
                    resources.getDimension(R.dimen.eight_dp_margin)
                )
                .setTopLeftCorner(
                    CornerFamily.ROUNDED,
                    resources.getDimension(R.dimen.eight_dp_margin)
                )
        }

        ivPreviewImage.shapeAppearanceModel = builder.build()
    }
}
