package com.clover.studio.exampleapp.utils.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import com.clover.studio.exampleapp.databinding.DialogErrorBinding
import com.clover.studio.exampleapp.utils.extendables.BaseDialog
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction

class DialogError(
    context: Context,
    private val title: String,
    private val description: String?,
    private val firstOption: String?,
    private val secondOption: String,
    private val listener: DialogInteraction
) : BaseDialog(context) {
    private var bindingSetup: DialogErrorBinding? = null

    private val binding get() = bindingSetup!!

    companion object {
        @Volatile
        private var INSTANCE: DialogError? = null

        @Synchronized
        fun getInstance(
            context: Context,
            title: String,
            description: String?,
            firstOption: String?,
            secondOption: String,
            listener: DialogInteraction
        ): DialogError = INSTANCE
            ?: DialogError(
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
        bindingSetup = DialogErrorBinding.inflate(layoutInflater)
        val view = bindingSetup!!.root
        setContentView(view)

        initViews()
    }

    private fun initViews() {
        binding.tvTextTitle.text = title

        if (description != null && description.isNotEmpty())
            binding.tvTextDescription.text = description
        else
            binding.tvTextDescription.visibility = View.GONE

        if (firstOption != null && firstOption.isNotEmpty())
            binding.btnFirstOption.text = firstOption
        else
            binding.btnFirstOption.visibility = View.INVISIBLE

        binding.btnSecondOption.text = secondOption

        binding.btnSecondOption.setOnClickListener {
            listener.onSecondOptionClicked()
            dismiss()
        }

        binding.btnFirstOption.setOnClickListener {
            listener.onFirstOptionClicked()
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