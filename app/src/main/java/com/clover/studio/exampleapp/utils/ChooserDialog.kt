package com.clover.studio.exampleapp.utils

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import com.clover.studio.exampleapp.databinding.DialogChooserBinding

class ChooserDialog(
    context: Context?,
    private val title: String,
    private val description: String?,
    private val firstOption: String,
    private val secondOption: String,
    private val listener: DialogInteraction,
) : AlertDialog(context) {
    private var bindingSetup: DialogChooserBinding? = null

    private val binding get() = bindingSetup!!

    companion object {
        @Volatile
        private var INSTANCE: ChooserDialog? = null

        @Synchronized
        fun getInstance(
            context: Context,
            title: String,
            description: String?,
            firstOption: String,
            secondOption: String,
            listener: DialogInteraction
        ): ChooserDialog = INSTANCE
            ?: ChooserDialog(
                context,
                title,
                description,
                firstOption,
                secondOption,
                listener
            ).also { INSTANCE = it }.also {
                it.window?.setBackgroundDrawableResource(android.R.color.transparent)
                it.show()
            }
    }

    interface DialogInteraction {
        fun onFirstOptionClicked()
        fun onSecondOptionClicked()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = DialogChooserBinding.inflate(layoutInflater)
        val view = bindingSetup!!.root
        setContentView(view)

        initViews()
    }

    private fun initViews() {
        binding.tvTextTitle.text = title
        if (description != null && description.isNotEmpty()) {
            binding.tvTextDescription.text = description
        } else binding.tvTextDescription.visibility = View.GONE
        binding.btnFirstOption.text = firstOption
        binding.btnSecondOption.text = secondOption

        binding.btnSecondOption.setOnClickListener {
            listener.onSecondOptionClicked()
            dismiss()
        }

        binding.btnFirstOption.setOnClickListener {
            listener.onFirstOptionClicked()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDetachedFromWindow() {
        INSTANCE = null
    }

    override fun onBackPressed() {
        dismiss()
    }
}