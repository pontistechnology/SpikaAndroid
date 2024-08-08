package com.clover.studio.spikamessenger.utils.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.DialogErrorBinding
import com.clover.studio.spikamessenger.utils.extendables.BaseDialog
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction


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

    private fun initViews() = with(binding) {

        tvTextTitle.text = title

        if (!description.isNullOrEmpty())
            tvTextDescription.text = description
        else
            tvTextDescription.visibility = View.GONE

        if (firstOption == null && secondOption == context.getString(R.string.ok)) {
            btnOneOption.visibility = View.VISIBLE
            btnOneOption.text = secondOption
            llTwoOptions.visibility = View.GONE

            btnOneOption.setOnClickListener {
                listener.onSecondOptionClicked()
                dismiss()
            }
        } else {
            llTwoOptions.visibility = View.VISIBLE
            btnOneOption.visibility = View.GONE

            if (!firstOption.isNullOrEmpty()) btnFirstOption.text = firstOption

            if (secondOption == context.getString(R.string.exit)) {
                btnSecondOption.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.warningColor
                    )
                )
            }

            btnSecondOption.text = secondOption

            btnSecondOption.setOnClickListener {
                listener.onSecondOptionClicked()
                dismiss()
            }

            btnFirstOption.setOnClickListener {
                listener.onFirstOptionClicked()
                dismiss()
            }
        }
    }

    override fun onDetachedFromWindow() {
        INSTANCE = null
    }

    override fun onBackPressed() {
        dismiss()
    }
}
