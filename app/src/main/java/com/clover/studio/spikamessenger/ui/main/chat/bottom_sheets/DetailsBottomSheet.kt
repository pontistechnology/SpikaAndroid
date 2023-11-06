package com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.MessageDetailsBinding
import com.clover.studio.spikamessenger.ui.main.chat.MessageDetailsAdapter
import com.clover.studio.spikamessenger.utils.Const
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DetailsBottomSheet(
    private val context: Context,
    private val roomWithUsers: RoomWithUsers,
    private val messagesRecords: MessageAndRecords,
    private var localUserId: Int,
) : BottomSheetDialogFragment() {

    private lateinit var binding: MessageDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MessageDetailsBinding.inflate(layoutInflater)
        binding.ivRemove.setOnClickListener {
            dismiss()
        }

        setDetails(context, messagesRecords)

        return binding.root
    }

    companion object {
        const val TAG = "detailsSheet"
    }

    private fun setDetails(context: Context, message: MessageAndRecords) = with(binding) {
        val detailsMessageAdapter = MessageDetailsAdapter(
            context,
            roomWithUsers,
        )
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        rvReactionsDetails.adapter = detailsMessageAdapter
        rvReactionsDetails.layoutManager = layoutManager
        rvReactionsDetails.itemAnimator = null

        val senderId = message.message.fromUserId

        /* Adding a message record for the sender so that it can be sent to the adapter */
        val senderMessageRecord = MessageRecords(
            id = 0,
            messageId = message.message.id,
            userId = message.message.fromUserId!!,
            type = Const.JsonFields.SENT,
            reaction = null,
            modifiedAt = message.message.modifiedAt,
            createdAt = message.message.createdAt!!,
            isDeleted = false,
            null
        )

        /* In the messageDetails list, we save message records for a specific message,
         remove reactions from those records(because we only need the seen and delivered types),
         remove the sender from the seen/delivered list and sort the list so that first we see
         seen and then delivered. */
        val messageDetails = message.records.orEmpty()
            .filter { it.type != Const.JsonFields.REACTION && it.userId != senderId }
            .sortedWith(compareByDescending<MessageRecords> { it.type }.thenByDescending { it.createdAt })
            .distinctBy { it.userId }
            .toMutableList()

        /* Then we add the sender of the message to the first position of the messageDetails list
        * so that wŽe can display it in the RecyclerView */
        messageDetails.add(0, senderMessageRecord)

        /* If the room type is a group and the current user is not the sender, remove it from the list.*/
        if ((Const.JsonFields.GROUP == roomWithUsers.room.type) && (senderId != localUserId)) {
            val filteredMessageDetails =
                messageDetails.filter { it.userId != localUserId }.toMutableList()
            detailsMessageAdapter.submitList(ArrayList(filteredMessageDetails))
        } else {
            detailsMessageAdapter.submitList(ArrayList(messageDetails))
        }
    }
}
