package com.clover.studio.spikamessenger.utils.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.ChatOptionItemBinding
import com.clover.studio.spikamessenger.databinding.MoreOptionsBinding
import com.clover.studio.spikamessenger.utils.extendables.BaseDialog
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction

class ChooserDialog(
    context: Context?,
    private val listChooseOptions: MutableList<String>,
    private val listener: DialogInteraction,
) : BaseDialog(context) {
    private var bindingSetup: MoreOptionsBinding? = null

    private val binding get() = bindingSetup!!

    companion object {
        @Volatile
        private var INSTANCE: ChooserDialog? = null

        @Synchronized
        fun getInstance(
            context: Context,
            listChooseOptions: MutableList<String>,
            listener: DialogInteraction
        ): ChooserDialog = INSTANCE
            ?: ChooserDialog(
                context,
                listChooseOptions,
                listener
            ).also { INSTANCE = it }.also {
                it.window?.setBackgroundDrawableResource(android.R.color.transparent)
                it.window?.setGravity(Gravity.BOTTOM)
                it.show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = MoreOptionsBinding.inflate(layoutInflater)
        val view = bindingSetup!!.root
        setContentView(view)

        initViews()
    }

    private fun initViews(){
        listChooseOptions.forEachIndexed { index, item ->
            val isFirstView = index == 0
            val isLastView = index == listChooseOptions.size - 1

            val newView: View =
                LayoutInflater.from(context).inflate(R.layout.chat_option_item, null)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )

            val marginInPixels = context.resources.getDimensionPixelSize(R.dimen.four_dp_margin)
            layoutParams.setMargins(0, 0, 0, marginInPixels)

            newView.layoutParams = layoutParams

            val newViewBinding = ChatOptionItemBinding.bind(newView)

            val textView =
                if (isFirstView) binding.tvFirstItem else if (isLastView) binding.tvLastItem else newViewBinding.tvOptionName
            textView.text = item

            val lp = textView.layoutParams as FrameLayout.LayoutParams
            lp.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
            newViewBinding.tvOptionName.layoutParams = lp

            val frameLayout =
                if (isFirstView) binding.flFirstItem else if (isLastView) binding.flLastItem else newViewBinding.flNewItem
            frameLayout.tag = index

            frameLayout.setOnClickListener {
                listener.onOptionClicked(item)
                dismiss()
            }

            if (index > 0 && index < listChooseOptions.size - 1) {
                binding.llOptions.addView(newView, index)
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
