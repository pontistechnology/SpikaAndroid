package com.clover.studio.exampleapp.utils.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.DialogChooserBinding
import com.clover.studio.exampleapp.utils.extendables.BaseDialog
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction

class ChooserDialog(
    context: Context?,
    private val title: String?,
    private val description: String?,
    private val firstOption: String,
    private val secondOption: String?,
    private val listener: DialogInteraction,
) : BaseDialog(context) {
    private var bindingSetup: DialogChooserBinding? = null

    private val binding get() = bindingSetup!!

    companion object {
        @Volatile
        private var INSTANCE: ChooserDialog? = null

        @Synchronized
        fun getInstance(
            context: Context,
            title: String?,
            description: String?,
            firstOption: String,
            secondOption: String?,
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = DialogChooserBinding.inflate(layoutInflater)
        val view = bindingSetup!!.root
        setContentView(view)

        initViews()
    }

    private fun initViews() {
        if (title != null && title.isNotEmpty()) {
            if (context.getString(R.string.delete) == title) {
                // Change colors for delete options
                binding.btnFirstOption.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.style_red
                    )
                )
                binding.btnSecondOption.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.style_red
                    )
                )
                binding.btnCancel.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.primary_color
                    )
                )
                binding.tvTextTitle.visibility = View.GONE
            }
            binding.tvTextTitle.text = title
        } else {
            binding.tvTextTitle.visibility = View.GONE
        }

        if (description != null && description.isNotEmpty())
            binding.tvTextDescription.text = description
        else
            binding.tvTextDescription.visibility = View.GONE

        if (secondOption != null && secondOption.isNotEmpty())
            binding.btnSecondOption.text = secondOption
        else
            binding.btnSecondOption.visibility = View.GONE

        binding.btnFirstOption.text = firstOption

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