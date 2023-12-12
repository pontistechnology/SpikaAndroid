package com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.entity.RoomWithMessage
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.databinding.BottomSheetForwardBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.contacts.ContactsAdapter
import com.clover.studio.spikamessenger.ui.main.create_room.SelectedContactsAdapter
import com.clover.studio.spikamessenger.ui.main.rooms.RoomsAdapter
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.helpers.ColorHelper
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortUsersByLocale
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber

const val MAX_CHATS_NUMBER = 3

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
    private lateinit var selectedAdapter: SelectedContactsAdapter

    private var recentChats: List<RoomWithMessage> = mutableListOf()
    private var recentContacts: List<RoomWithMessage> = mutableListOf()
    private var userList: List<UserAndPhoneUser> = mutableListOf()
    private var selectedChats: MutableList<UserAndPhoneUser> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetForwardBinding.inflate(layoutInflater)

        initializeLists()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setUpSelectedAdapter()
    }

    companion object {
        const val TAG = "forwardSheet"
    }

    interface BottomSheetForwardAction {
        fun forward(userId: ArrayList<Int>?, roomId: ArrayList<Int>)
    }

    fun setForwardListener(listener: BottomSheetForwardAction) {
        this.listener = listener
    }

    private fun initializeLists() = with(binding) {
        viewModel.getUserAndPhoneUser(localId).observe(viewLifecycleOwner) {
            when (it.status) {
                Resource.Status.SUCCESS -> {

                    if (it.responseData != null) {
                        userList = it.responseData.toMutableList().sortUsersByLocale(context)

                        Timber.d("User list: $userList")

                        viewModel.getRecentMessages(Const.JsonFields.PRIVATE)
                            .observe(viewLifecycleOwner) { recentMessages ->
                                if (!recentMessages.responseData.isNullOrEmpty()) {
                                    recentContacts = recentMessages.responseData
                                        .filter { user -> user.roomWithUsers.room.type == Const.JsonFields.PRIVATE }
                                        .take(MAX_CHATS_NUMBER)

                                    pbForward.visibility = View.GONE
                                    nsvForward.visibility = View.VISIBLE

                                    setUpContactsAdapter()
                                }
                            }
                    }
                }

                Resource.Status.LOADING -> {
                    pbForward.visibility = View.VISIBLE
                    nsvForward.visibility = View.GONE
                }

                Resource.Status.ERROR -> {
                    // TODO ask Matko
                    // Maybe toast with "Something went wrong" and dismiss it
                    dismiss()
                }

                else -> Timber.d("Other error::: ${it.status}, ${it.responseData}")
            }
        }
    }


    private fun initializeViews() = with(binding) {
        btnContacts.setOnClickListener {
            btnContacts.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getFourthAdditionalColor(context))
            btnGroups.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getPrimaryColor(context))

            rvContacts.visibility = View.VISIBLE
        }


        btnGroups.setOnClickListener {
            btnGroups.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getFourthAdditionalColor(context))
            btnContacts.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getPrimaryColor(context))

            rvContacts.visibility = View.INVISIBLE
        }

        fabForward.setOnClickListener {
            val userIds = arrayListOf<Int>()
            val roomIds = arrayListOf(0)

            selectedChats.forEach {
                userIds.add(it.user.id)
//                roomIds.add()
            }

            listener?.forward(userIds, roomIds)
            dismiss()
        }
    }

    private fun setUpSelectedAdapter() = with(binding) {
        selectedAdapter = SelectedContactsAdapter(
            context
        ) {
            selectedChats.remove(it)
            it.user.selected = false
            contactsAdapter.notifyDataSetChanged()
            selectedAdapter.notifyDataSetChanged()

            if (selectedChats.isEmpty()) {
                rvSelected.visibility = View.GONE
                fabForward.visibility = View.VISIBLE
            }
        }

        rvSelected.apply {
            adapter = selectedAdapter
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        }
    }

    private fun setUpContactsAdapter()  = with(binding){
        val list: List<UserAndPhoneUser> = recentContacts
            .flatMap { it.roomWithUsers.users }
            .mapNotNull { user -> userList.find { it.user.id == user.id } }

        list.forEach {
            it.user.isForwarded = true
        }

        userList = list + userList

        contactsAdapter = ContactsAdapter(
            context = requireContext(),
            isGroupCreation = false,
            userIdsInRoom = null,
            isForward = true
        )
        {
            if (!selectedChats.contains(it)) {
                selectedChats.add(it)
                selectedAdapter.submitList(selectedChats)
                selectedAdapter.notifyDataSetChanged()

                it.user.selected = true
                contactsAdapter.notifyDataSetChanged()
            }
            rvSelected.visibility = View.VISIBLE
        }

        rvContacts.apply {
            adapter = contactsAdapter
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        }

        contactsAdapter.submitList(userList)
    }
}
