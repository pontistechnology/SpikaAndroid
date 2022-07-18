package com.clover.studio.exampleapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.ItemImageSelectedBinding
import timber.log.Timber

class ImageSelectedContainer(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    private var removeImageSelected: RemoveImageSelected? = null
    private var bindingSetup: ItemImageSelectedBinding = ItemImageSelectedBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup

    init {
        handleButtonClicks()
    }

    interface RemoveImageSelected {
        fun removeImage()
    }

    fun setButtonListener(removeImageSelected: RemoveImageSelected?) {
        this.removeImageSelected = removeImageSelected
    }

    fun setImage(bitmap: Bitmap) {
        binding.ivUserImage.let { Glide.with(this).load(bitmap).into(it) }
    }

    private fun handleButtonClicks() {
        binding.ivRemoveImage.setOnClickListener { removeImageSelected!!.removeImage() }
    }
}