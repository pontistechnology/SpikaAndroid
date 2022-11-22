package com.clover.studio.exampleapp.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.ReactionsBinding

class ReactionsContainer(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    private var listener: AddReaction? = null
    private var bindingSetup: ReactionsBinding = ReactionsBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup

    init {
        handleButtonClicks()
    }

    interface AddReaction {
        fun addReaction(reaction: String)
    }

    fun setButtonListener(listener: AddReaction?) {
        this.listener = listener
    }

    private fun handleButtonClicks() {
        binding.clEmoji.children.forEach { child ->
            child.setOnClickListener {
                when (child) {
                    binding.tvThumbsUpEmoji -> {
                        listener!!.addReaction(context.getString(R.string.thumbs_up_emoji))
                    }
                    binding.tvHeartEmoji -> {
                        listener!!.addReaction(context.getString(R.string.heart_emoji))
                    }
                    binding.tvCryingEmoji -> {
                        listener!!.addReaction(context.getString(R.string.crying_face_emoji))
                    }
                    binding.tvAstonishedEmoji -> {
                        listener!!.addReaction(context.getString(R.string.astonished_emoji))
                    }
                    binding.tvDisappointedRelievedEmoji -> {
                        listener!!.addReaction(context.getString(R.string.disappointed_relieved_emoji))
                    }
                    binding.tvPrayingHandsEmoji -> {
                        listener!!.addReaction(context.getString(R.string.praying_hands_emoji))
                    }
                }
            }
        }
    }
}