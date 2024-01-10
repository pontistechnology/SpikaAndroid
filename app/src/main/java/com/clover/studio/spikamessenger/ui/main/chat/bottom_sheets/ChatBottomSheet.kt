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
        fun actionAddCustomReaction()
        fun actionDownload()
        fun actionForward()
    }

    fun setActionListener(listener: BottomSheetAction?) {
        this.listener = listener
    }

    companion object {
        const val TAG = "chatBottomSheet"
    }

    private fun setFunctionsVisibility() = with(binding) {
        tvDelete.visibility =
            if (message.fromUserId == localId) View.VISIBLE else View.GONE

        tvEdit.visibility =
            if (message.fromUserId == localId && Const.JsonFields.TEXT_TYPE == message.type && !message.isForwarded) View.VISIBLE else View.GONE

        tvCopy.visibility =
            if (Const.JsonFields.TEXT_TYPE == message.type) View.VISIBLE else View.GONE

        tvDownload.visibility =
            if (Const.JsonFields.IMAGE_TYPE == message.type) View.VISIBLE else View.GONE
    }

    private fun handleBottomSheetAction() = with(binding) {
        cvDeleted.setOnClickListener {
            listener?.actionDelete()
            dismiss()
        }

        cvEdited.setOnClickListener {
            listener?.actionEdit()
            dismiss()
        }

        cvReply.setOnClickListener {
            listener?.actionReply()
            dismiss()
        }

        cvDetails.setOnClickListener {
            listener?.actionDetails()
            dismiss()
        }

        cvCopy.setOnClickListener {
            listener?.actionCopy()
            dismiss()
        }

        cvDownload.setOnClickListener {
            listener?.actionDownload()
            dismiss()
        }

        cvForward.setOnClickListener {
            listener?.actionForward()
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
                listener?.actionAddCustomReaction()
                dismiss()
            }
        })
    }
}
