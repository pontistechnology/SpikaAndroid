package com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clover.studio.spikamessenger.databinding.BottomSheetCustomReactionBinding
import com.clover.studio.spikamessenger.utils.Tools
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CustomReactionBottomSheet(
    private val context: Context
) :
    BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetCustomReactionBinding
    private var listener: BottomSheetCustomReactionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetCustomReactionBinding.inflate(layoutInflater)

        initializeViews()


        return binding.root
    }

    companion object {
        const val TAG = "customReactionSheet"
    }

    interface BottomSheetCustomReactionListener {
        fun addCustomReaction(emoji: String)
    }

    fun setCustomReactionListener(listener: BottomSheetCustomReactionListener) {
        this.listener = listener
    }

    private fun initializeViews() {
        val emojiView = binding.view
        emojiView.setUp(
            rootView = binding.view,
            onEmojiClickListener = {
                listener?.addCustomReaction(it.unicode)
                dismiss()
            },
            onEmojiBackspaceClickListener = null,
            editText = null,
            theming = Tools.setEmojiViewTheme(context)
        )
    }
}
