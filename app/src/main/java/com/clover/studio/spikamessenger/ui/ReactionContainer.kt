package com.clover.studio.spikamessenger.ui

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.clover.studio.spikamessenger.databinding.ReactionBinding

class ReactionContainer(context: Context, attrs: AttributeSet?, reaction: String, reactionNumber: String) :
    ConstraintLayout(context, attrs) {
    private var reaction : String = ""
    private var reactionNumber : String = ""
    private var bindingSetup: ReactionBinding = ReactionBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup

    init {
        this.reaction = reaction
        this.reactionNumber = reactionNumber
        handleSettingReaction()
    }

    fun showReaction() : String{
        return reaction
    }

    private fun handleSettingReaction(){
        binding.tvReaction.text = reaction
        val spanStringBuilder = SpannableStringBuilder(reactionNumber)
        spanStringBuilder.setSpan(
            RelativeSizeSpan(0.8f),
            reactionNumber.length - 1,
            reactionNumber.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.tvReactionNumber.text = spanStringBuilder.append(" ")
    }
}
