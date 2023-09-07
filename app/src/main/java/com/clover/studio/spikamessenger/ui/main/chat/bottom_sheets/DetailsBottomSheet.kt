package com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.entity.Message
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.databinding.MessageDetailsBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.ui.main.chat.MessageDetailsAdapter
import com.clover.studio.spikamessenger.utils.Const
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DetailsBottomSheet(
    private val context: Context,
    private val message: Message,
) : BottomSheetDialogFragment() {

    private lateinit var binding: MessageDetailsBinding
    private val viewModel: ChatViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MessageDetailsBinding.inflate(layoutInflater)
        binding.ivRemove.setOnClickListener {
            dismiss()
        }

        setDetails(context, message)

        return binding.root
    }

    companion object {
        const val TAG = "detailsSheet"
    }

    private fun setDetails(context: Context, message: Message) {
        val detailsMessageAdapter = MessageDetailsAdapter(
            context,
            viewModel.roomWithUsers.value,
        )
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        binding.rvReactionsDetails.adapter = detailsMessageAdapter
        binding.rvReactionsDetails.layoutManager = layoutManager
        binding.rvReactionsDetails.itemAnimator = null

        val senderId = message.fromUserId

        /* Adding a message record for the sender so that it can be sent to the adapter */
        val senderMessageRecord = MessageRecords(
            id = 0,
            messageId = message.id,
            userId = message.fromUserId!!,
            type = Const.JsonFields.SENT,
            reaction = null,
            modifiedAt = message.modifiedAt,
            createdAt = message.createdAt!!,
            null
        )

        /* In the messageDetails list, we save message records for a specific message,
         remove reactions from those records(because we only need the seen and delivered types),
         remove the sender from the seen/delivered list and sort the list so that first we see
         seen and then delivered. */
        val messageDetails =
            viewModel.messagesRecords.filter { it.message.id == message.id }
                .flatMap { it.records!! }
                .filter { Const.JsonFields.REACTION != it.type }
                .filter { it.userId != message.fromUserId }
                .sortedByDescending { it.type }
                .toMutableList()

        /* Then we add the sender of the message to the first position of the messageDetails list
        * so that we can display it in the RecyclerView */
        messageDetails.add(0, senderMessageRecord)

        /* If the room type is a group and the current user is not the sender, remove it from the list.*/
        if ((Const.JsonFields.GROUP == viewModel.roomWithUsers.value?.room?.type) && (senderId != viewModel.getLocalUserId())) {
            val filteredMessageDetails =
                messageDetails.filter { it.userId != viewModel.getLocalUserId() }.toMutableList()
            detailsMessageAdapter.submitList(ArrayList(filteredMessageDetails))
        } else {
            detailsMessageAdapter.submitList(ArrayList(messageDetails))
        }
    }
}
