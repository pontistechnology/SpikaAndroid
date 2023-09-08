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
import com.clover.studio.spikamessenger.data.models.junction.RoomWithUsers
import com.clover.studio.spikamessenger.databinding.BottomSheetReactionsDetailsBinding
import com.clover.studio.spikamessenger.ui.ReactionContainer
import com.clover.studio.spikamessenger.ui.main.chat.MessageReactionAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ReactionsBottomSheet(
    private val context: Context,
    private val message: MessageAndRecords,
    private val roomWithUsers: RoomWithUsers
) :
    BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetReactionsDetailsBinding

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

    private fun setReactions(messageRecords: MessageAndRecords) {
        val messageReactionAdapter = MessageReactionAdapter(
            context,
            roomWithUsers,
        )

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.rvReactionsDetails.adapter = messageReactionAdapter
        binding.rvReactionsDetails.layoutManager = layoutManager
        binding.rvReactionsDetails.itemAnimator = null


        binding.tvAllReactions.setBackgroundResource(R.drawable.bg_reaction_selected)

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
                binding.llReactions.addView(reactionView)
            }
        }

        // Set listeners to reaction views and submit new, filtered list of reactions to adapter
        var currentlySelectedTextView: View? = null
        for (child in binding.llReactions.children) {
            child.setOnClickListener { view ->
                // Remove / add backgrounds for views
                if (view != currentlySelectedTextView) {
                    currentlySelectedTextView?.background = null
                    view.setBackgroundResource(R.drawable.bg_reaction_selected)
                    currentlySelectedTextView = view
                }

                val childIndex = binding.llReactions.indexOfChild(view)
                if (childIndex == 0) {
                    messageReactionAdapter.submitList(reactionsList)
                } else {
                    binding.tvAllReactions.background = null
                    val reactionView: ReactionContainer =
                        binding.llReactions.getChildAt(childIndex) as ReactionContainer
                    val reactionText = reactionView.showReaction()
                    messageReactionAdapter.submitList(reactionsList.filter { it.reaction == reactionText })
                }
            }
        }
    }
}
