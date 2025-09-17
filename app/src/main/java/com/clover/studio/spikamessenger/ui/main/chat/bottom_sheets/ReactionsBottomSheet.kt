package com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.MessageAndRecords
import com.clover.studio.spikamessenger.data.models.entity.MessageRecords
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.BottomSheetReactionsDetailsBinding
import com.clover.studio.spikamessenger.ui.ReactionContainer
import com.clover.studio.spikamessenger.ui.main.chat.MessageReactionAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ReactionsBottomSheet(
    private val context: Context,
    private val message: MessageAndRecords,
    private val roomWithUsers: RoomWithUsers,
    private val localUserId: Int,
) :
    BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetReactionsDetailsBinding
    private var listener: ReactionsAction? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetReactionsDetailsBinding.inflate(layoutInflater)
        binding.ivRemove.setOnClickListener {
            dismiss()
        }

        setReactions(message)

        return binding.root
    }

    companion object {
        const val TAG = "reactionsSheet"
    }

    interface ReactionsAction {
        fun deleteReaction(reaction: MessageRecords?)
    }

    fun setReactionListener(listener: ReactionsAction?) {
        this.listener = listener
    }

    private fun setReactions(messageRecords: MessageAndRecords) = with(binding) {
        val messageReactionAdapter = MessageReactionAdapter(
            context = context,
            roomWithUsers = roomWithUsers,
            localUserId = localUserId,
            deleteReaction = { reactionToDelete ->
                listener?.deleteReaction(reactionToDelete)
                dismiss()
            }
        )

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rvReactionsDetails.adapter = messageReactionAdapter
        rvReactionsDetails.layoutManager = layoutManager
        rvReactionsDetails.itemAnimator = null

        tvAllReactions.setBackgroundResource(R.drawable.bg_reaction_selected)

        val reactionsList = messageRecords.records!!.filter { it.reaction != null }
            .sortedByDescending { it.createdAt }
        // Default view - all reactions:
        messageReactionAdapter.submitList(reactionsList)

        // Group same reactions
        val reactionList = reactionsList.groupBy { it.reaction }.mapValues { it.value.size }

        // Add reaction views:
        if (reactionList.isNotEmpty()) {
            for (reaction in reactionList) {
                val reactionView = ReactionContainer(
                    requireActivity(),
                    null,
                    reaction.key.toString(),
                    reaction.value.toString()
                )
                llReactions.addView(reactionView)
            }
        }

        // Set listeners to reaction views and submit new, filtered list of reactions to adapter
        var currentlySelectedTextView: View? = null
        for (child in llReactions.children) {
            child.setOnClickListener { view ->
                // Remove / add backgrounds for views
                if (view != currentlySelectedTextView) {
                    currentlySelectedTextView?.background = null
                    view.setBackgroundResource(R.drawable.bg_reaction_selected)
                    currentlySelectedTextView = view
                }

                val childIndex = llReactions.indexOfChild(view)
                if (childIndex == 0) {
                    messageReactionAdapter.submitList(reactionsList)
                } else {
                    tvAllReactions.background = null
                    val reactionView: ReactionContainer =
                        llReactions.getChildAt(childIndex) as ReactionContainer
                    val reactionText = reactionView.showReaction()
                    messageReactionAdapter.submitList(reactionsList.filter { it.reaction == reactionText })
                }
            }
        }
    }
}
