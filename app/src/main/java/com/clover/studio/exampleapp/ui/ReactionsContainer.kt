package com.clover.studio.exampleapp.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.EmojiBinding

// TODO - Custom emoji view
class ReactionsContainer(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    var reaction = ""
    private var bindingSetup: EmojiBinding = EmojiBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup

    init {
        handleButtonClicks()
    }

    private fun handleButtonClicks(): String {
        binding.clEmoji.children.forEach { child ->
            child.setOnClickListener {
                when (child) {
                    binding.tvThumbsUpEmoji -> {
                        reaction = context.getString(R.string.thumbs_up_emoji)
                    }
                    binding.tvHeartEmoji -> {
                        reaction = context.getString(R.string.heart_emoji)
                    }
                    binding.tvCryingEmoji -> {
                        reaction = context.getString(R.string.crying_face_emoji)
                    }
                    binding.tvAstonishedEmoji -> {
                        reaction = context.getString(R.string.astonished_emoji)
                    }
                    binding.tvDisappointedRelievedEmoji -> {
                        reaction = context.getString(R.string.disappointed_relieved_emoji)

                    }
                    binding.tvPrayingHandsEmoji -> {
                        reaction = context.getString(R.string.praying_hands_emoji)
                    }
                }
            }
        }
        return reaction
    }
}