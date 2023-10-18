package com.clover.studio.spikamessenger.utils.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.DialogChooserBinding
import com.clover.studio.spikamessenger.utils.extendables.BaseDialog
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction

class ChooserDialog(
    context: Context?,
    private val title: String?,
    private val description: String?,
    private val firstOption: String?,
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
            firstOption: String?,
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
                it.window?.setGravity(Gravity.BOTTOM)
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

    private fun initViews() = with(binding) {
        if (!title.isNullOrEmpty()) {
            if (context.getString(R.string.delete) == title) {
//                btnFirstOption.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.style_red
//                    )
//                )
//                btnSecondOption.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.style_red
//                    )
//                )
//                btnCancel.setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        R.color.primary_color
//                    )
//                )
                tvTextTitle.visibility = View.GONE
            }
            tvTextTitle.text = title
        } else {
            tvTextTitle.visibility = View.GONE
        }

        if (!description.isNullOrEmpty())
            tvTextDescription.text = description
        else
            tvTextDescription.visibility = View.GONE

        if (!secondOption.isNullOrEmpty()) {
            btnSecondOption.text = secondOption
            vBorder.visibility = View.VISIBLE
        } else {
            btnSecondOption.visibility = View.GONE
            vBorder.visibility = View.GONE
        }

        btnFirstOption.text = firstOption
        btnFirstOption.setOnClickListener {
            listener.onFirstOptionClicked()
            dismiss()
        }

        btnSecondOption.setOnClickListener {
            listener.onSecondOptionClicked()
            dismiss()
        }

        btnCancel.setOnClickListener {
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
