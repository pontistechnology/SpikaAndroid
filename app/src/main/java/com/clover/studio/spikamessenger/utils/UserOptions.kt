package com.clover.studio.spikamessenger.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.ChatOptionItemBinding
import com.clover.studio.spikamessenger.databinding.MoreOptionsBinding


class UserOptions(context: Context) :
    ConstraintLayout(context) {

    private var bindingSetup: MoreOptionsBinding = MoreOptionsBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup
    private var listener: OptionsListener? = null

    interface OptionsListener {
        fun clickedOption(optionName: Int)
    }

    fun setOptionsListener(listener: OptionsListener?) {
        this.listener = listener
    }

    fun setOptions(optionList: MutableList<Pair<String, Drawable?>>) {
        optionList.forEachIndexed { index, item ->
            when (index) {
                0 -> {
                    binding.tvFirstItem.text = item.first
                    binding.flFirstItem.apply {
                        tag = 0
                        setOnClickListener {
                            listener?.clickedOption(tag as Int)
                        }
                    }
                }

                optionList.size - 1 -> {
                    binding.tvLastItem.text = item.first
                    binding.flLastItem.apply {
                        tag = optionList.size - 1
                        setOnClickListener {
                            listener?.clickedOption(tag as Int)
                        }
                    }
                }

                else -> {
                    val newView: View =
                        LayoutInflater.from(context).inflate(R.layout.chat_option_item, null)

                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )

                    val marginInPixels = resources.getDimensionPixelSize(R.dimen.eight_dp_margin)
                    layoutParams.setMargins(0, 0, 0, marginInPixels)

                    newView.layoutParams = layoutParams

                    val newViewBinding = ChatOptionItemBinding.bind(newView)

                    newViewBinding.tvOptionName.text = item.first
                    newViewBinding.flNewItem.apply {
                        tag = index
                        setOnClickListener {
                            listener?.clickedOption(tag as Int)
                        }
                    }
                    binding.llOptions.addView(newView, index)
                }
            }
        }
    }
}
