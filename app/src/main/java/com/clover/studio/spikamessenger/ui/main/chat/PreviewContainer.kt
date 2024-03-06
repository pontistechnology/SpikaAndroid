package com.clover.studio.spikamessenger.ui.main.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
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
import timber.log.Timber

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
//        binding.clMessagePreview.visibility = View.GONE
    }

    fun closeBottomSheet() {
        binding.clMessagePreview.visibility = View.GONE
    }

    fun isPreviewBottomSheetVisible(): Boolean {
        return binding.clMessagePreview.visibility == View.VISIBLE
    }

    fun setPreviewContainer(thumbnailData: ThumbnailData) = with(binding) {
        Timber.d("Thumb data = ${thumbnailData.title}, ${thumbnailData.description}, ${thumbnailData.image}")
        tvTitle.text = thumbnailData.title
        tvDescription.text = thumbnailData.description

        Glide.with(context)
            .load(thumbnailData.image)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .placeholder(R.drawable.img_image_placeholder)
            .dontTransform()
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
