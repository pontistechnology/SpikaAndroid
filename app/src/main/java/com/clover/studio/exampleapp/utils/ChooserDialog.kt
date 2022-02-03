package com.clover.studio.exampleapp.utils

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.DialogChooserBinding

class ChooserDialog(
    context: Context?,
    private val listener: DialogInteraction
) : AlertDialog(context) {
    private var bindingSetup: DialogChooserBinding? = null

    private val binding get() = bindingSetup!!

    companion object {
        @Volatile
        private var INSTANCE: ChooserDialog? = null

        @Synchronized
        fun getInstance(context: Context, listener: DialogInteraction): ChooserDialog = INSTANCE
            ?: ChooserDialog(context, listener).also { INSTANCE = it }.also {
                it.show()
            }
    }

    interface DialogInteraction {
        fun onPhotoClicked()
        fun onGalleryClicked()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = DialogChooserBinding.inflate(layoutInflater)
        val view = bindingSetup!!.root
        setContentView(view)

        initButtons()
    }

    private fun initButtons() {
        binding.btnSelectPhoto.setOnClickListener {
            listener.onGalleryClicked()
            dismiss()
        }

        binding.btnTakePhoto.setOnClickListener {
            listener.onPhotoClicked()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDetachedFromWindow() {
        INSTANCE = null
    }
}