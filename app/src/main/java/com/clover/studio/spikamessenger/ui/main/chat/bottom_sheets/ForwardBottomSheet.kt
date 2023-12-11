package com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.RoomWithMessage
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.databinding.BottomSheetForwardBinding
import com.clover.studio.spikamessenger.databinding.ItemUserForwardBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.contacts.ContactsAdapter
import com.clover.studio.spikamessenger.ui.main.rooms.RoomsAdapter
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortUsersByLocale
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

const val RECENT_CHATS_NUMBER = 3

class ForwardBottomSheet(
    private val context: Context,
    private val localId: Int,
) :
    BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetForwardBinding
    private var listener: BottomSheetForwardAction? = null
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var roomsAdapter: RoomsAdapter

    private var recentChats: List<RoomWithMessage> = mutableListOf()
    private var recentContacts: List<RoomWithMessage> = mutableListOf()
    private var userList: List<UserAndPhoneUser> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetForwardBinding.inflate(layoutInflater)

        initializeLists()
        initializeViews()

        return binding.root
    }

    companion object {
        const val TAG = "forwardSheet"
    }

    interface BottomSheetForwardAction {
        fun forwardMessage()
    }

    fun setForwardListener(listener: BottomSheetForwardAction) {
        this.listener = listener
    }

    private fun initializeLists() {
        viewModel.getUserAndPhoneUser(localId).observe(viewLifecycleOwner) {
            if (it.responseData != null) {
                userList = it.responseData.toMutableList().sortUsersByLocale(context)
                setUpContactsAdapter()
            }
        }

        // Use custom view adding for it
        viewModel.getRecentMessages().observe(viewLifecycleOwner) {
            if (!it.responseData.isNullOrEmpty()) {
                binding.llRecentChats.visibility = View.VISIBLE
                recentContacts = it.responseData
                    .filter { user -> user.roomWithUsers.room.type == Const.JsonFields.PRIVATE }
                recentChats = it.responseData
                    .filter { user -> user.roomWithUsers.room.type == Const.JsonFields.GROUP }

                setUpRecentChats(recentContacts, isPrivate = true)
            }
        }
    }

    private fun initializeViews() {
        binding.btnContacts.setOnClickListener {
            setUpRecentChats(recentContacts, isPrivate = true)
            setUpContactsAdapter()
        }


        binding.btnGroups.setOnClickListener {
            setUpRecentChats(recentChats, isPrivate = false)
            setUpRoomsAdapter()
        }
    }

    private fun setUpRecentChats(recentContacts: List<RoomWithMessage>, isPrivate: Boolean) {
        binding.llRecentChats.removeAllViews()
        recentContacts.forEachIndexed { index, roomWithMessage ->
            if (index == RECENT_CHATS_NUMBER) {
                return
            }

            val newView: View =
                LayoutInflater.from(context).inflate(R.layout.item_user_forward, null)

            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )

            val marginInPixels = resources.getDimensionPixelSize(R.dimen.eight_dp_margin)
            layoutParams.setMargins(0, 0, 0, marginInPixels)
            newView.layoutParams = layoutParams

            val newViewBinding = ItemUserForwardBinding.bind(newView)

            newViewBinding.tvNumberPlaceholder.apply {
                if (isPrivate) {
                    visibility = View.VISIBLE
                    text =
                        roomWithMessage.roomWithUsers.users.find { it.id != localId }?.telephoneNumber
                } else {
                    visibility = View.GONE
                }
            }
            newViewBinding.tvNamePlaceholder.text = roomWithMessage.roomWithUsers.room.name
            binding.llRecentChats.addView(newView)
        }
    }

    private fun setUpRoomsAdapter() {
        roomsAdapter = RoomsAdapter(
            context = context,
            myUserId = localId.toString(),
            onItemClick = {
                // TODO method for group chat clicks
            }
        )

        roomsAdapter.submitList(recentChats)
        binding.rvRecentChats.apply {
            adapter = roomsAdapter
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        }

    }

    private fun setUpContactsAdapter() {
        contactsAdapter = ContactsAdapter(
            context = requireContext(),
            isGroupCreation = false,
            userIdsInRoom = null,
            isForward = true)
        {
            // TODO method for private chat clicks
        }

        binding.rvRecentChats.apply {
            adapter = contactsAdapter
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        }

        contactsAdapter.submitList(userList)
    }
}
