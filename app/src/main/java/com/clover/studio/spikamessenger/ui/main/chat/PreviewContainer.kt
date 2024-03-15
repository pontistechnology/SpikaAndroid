package com.clover.studio.spikamessenger.ui.main.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.networking.responses.ThumbnailData
import com.clover.studio.spikamessenger.databinding.PreviewActionBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel

class PreviewContainer(context: Context) : ConstraintLayout(context) {
    private var bindingSetup =
        PreviewActionBinding.inflate(LayoutInflater.from(context), this, true)
    private val binding get() = bindingSetup

    private var listener: PreviewContainerListener? = null

    interface PreviewContainerListener {
        fun closeSheet()
    }

    fun setPreviewContainerListener(listener: PreviewContainerListener) {
        this.listener = listener
    }

    init {
        binding.ivRemove.setOnClickListener {
            binding.clMessagePreview.visibility = View.GONE
            listener?.closeSheet()
        }
    }

    fun closeBottomSheet() {
        binding.clMessagePreview.visibility = View.GONE
    }

    fun setLoadingPreviewContainer(title: String) = with(binding) {
        clMessagePreview.visibility = VISIBLE

        tvTitle.text = title
        tvDescription.visibility = GONE

        ivPreviewImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    fun setPreviewContainer(thumbnailData: ThumbnailData) = with(binding) {
        tvTitle.text = thumbnailData.title
        tvDescription.text = thumbnailData.description

        tvDescription.visibility = VISIBLE

        ivPreviewImage.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(context)
            .load(thumbnailData.image)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .placeholder(R.drawable.img_image_placeholder)
            .dontTransform()
            .error(R.drawable.img_image_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(ivPreviewImage)
        setAppearanceModel(ivPreviewImage)
    }

    private fun setAppearanceModel(imageView: ShapeableImageView) {
        val shape = ShapeAppearanceModel().toBuilder()

        shape.setTopLeftCorner(
            CornerFamily.ROUNDED,
            resources.getDimension(R.dimen.eight_dp_margin)
        )

        imageView.shapeAppearanceModel = shape.build()
    }
}
