package com.clover.studio.exampleapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.ItemImageSelectedBinding
import com.clover.studio.exampleapp.utils.Const

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
        binding.ivUserImage.let { Glide.with(this).load(bitmap).centerCrop().into(it) }
    }

    fun setFile(extension: String, name: String) {
        binding.clFileDetails.visibility = View.VISIBLE
        binding.ivUserImage.visibility = View.GONE

        binding.tvFileName.text = name

        when (extension) {
            Const.FileExtensions.PDF -> binding.ivFile.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.img_pdf_black,
                    null
                )
            )
            Const.FileExtensions.ZIP -> binding.ivFile.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.img_zip,
                    null
                )
            )
            Const.FileExtensions.AUDIO -> binding.ivFile.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.img_audio_file,
                    null
                )
            )
            else -> binding.ivFile.setImageDrawable(
                ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.img_file_black,
                    null
                )
            )
        }
    }

    private fun handleButtonClicks() {
        binding.ivRemoveImage.setOnClickListener { removeImageSelected!!.removeImage() }
    }

    fun setMaxProgress(progress: Int) {
        binding.progressBar.max = progress
        binding.clProgressScreen.visibility = View.VISIBLE
        binding.ivRemoveImage.visibility = View.GONE
    }

    fun setUploadProgress(progress: Int) {
        binding.progressBar.secondaryProgress = progress
    }

    fun hideProgressScreen() {
        binding.clProgressScreen.visibility = View.GONE
    }
}