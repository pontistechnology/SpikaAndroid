package com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.databinding.BottomSheetMessageActionsBinding
import com.clover.studio.spikamessenger.ui.ReactionsContainer
import com.clover.studio.spikamessenger.utils.Const
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChatBottomSheet(
    private val message: Message,
    private var localId: Int,
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetMessageActionsBinding
    private var listener: BottomSheetAction? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetMessageActionsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleBottomSheetAction()
        setFunctionsVisibility()
    }

    interface BottomSheetAction {
        fun actionCopy()
        fun actionClose()
        fun actionDelete()
        fun actionEdit()
        fun actionReply()
        fun actionDetails()
        fun actionReaction(reaction: String)
        fun addCustomReaction()
    }

    fun setActionListener(listener: BottomSheetAction?) {
        this.listener = listener
    }

    companion object {
        const val TAG = "chatBottomSheet"
    }

    private fun setFunctionsVisibility() {
        binding.tvDelete.visibility =
            if (message.fromUserId == localId) View.VISIBLE else View.GONE

        binding.tvEdit.visibility =
            if (message.fromUserId == localId && Const.JsonFields.TEXT_TYPE == message.type) View.VISIBLE else View.GONE

        binding.tvCopy.visibility =
            if (Const.JsonFields.TEXT_TYPE == message.type) View.VISIBLE else View.GONE
    }

    private fun handleBottomSheetAction() {
        binding.tvDelete.setOnClickListener {
            listener?.actionDelete()
            dismiss()
        }

        binding.tvEdit.setOnClickListener {
            listener?.actionEdit()
            dismiss()
        }

        binding.tvReply.setOnClickListener {
            listener?.actionReply()
            dismiss()
        }

        binding.tvDetails.setOnClickListener {
            listener?.actionDetails()
            dismiss()
        }

        binding.tvCopy.setOnClickListener {
            listener?.actionCopy()
            dismiss()
        }

        val reactionsContainer = ReactionsContainer(requireContext(), null)
        binding.reactionsContainer.addView(reactionsContainer)
        reactionsContainer.setButtonListener(object : ReactionsContainer.AddReaction {
            override fun addReaction(reaction: String) {
                if (reaction.isNotEmpty()) {
                    listener?.actionReaction(reaction)
                    dismiss()
                }
            }

            override fun addCustomReaction() {
                listener?.addCustomReaction()
                dismiss()
            }
        })
    }
}
