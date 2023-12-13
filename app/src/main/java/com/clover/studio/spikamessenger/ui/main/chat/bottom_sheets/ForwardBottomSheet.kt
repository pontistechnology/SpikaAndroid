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
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.databinding.BottomSheetForwardBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.contacts.ContactsAdapter
import com.clover.studio.spikamessenger.ui.main.create_room.SelectedContactsAdapter
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.helpers.ColorHelper
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortPrivateGroupChats
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import timber.log.Timber

class ForwardBottomSheet(
    private val context: Context,
    private val localId: Int,
) :
    BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetForwardBinding
    private var listener: BottomSheetForwardAction? = null
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var selectedAdapter: SelectedContactsAdapter

    private var groupList: List<PrivateGroupChats> = mutableListOf()
    private var recentContacts: List<PrivateGroupChats> = mutableListOf()
    private var userList: List<PrivateGroupChats> = mutableListOf()
    private var selectedChats: MutableList<PrivateGroupChats> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetForwardBinding.inflate(layoutInflater)

        setUpAdapter()
        setUpSelectedAdapter()
        initializeLists()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
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

    private fun setUpAdapter() = with(binding) {
        contactsAdapter = ContactsAdapter(
            context = requireContext(),
            isGroupCreation = false,
            userIdsInRoom = null,
            isForward = true
        )
        {
            if (!selectedChats.contains(it)) {
                selectedChats.add(it)
                selectedAdapter.submitList(selectedChats.toMutableList())

                it.isSelected = true
                if (contactsAdapter.currentList == userList) {
                    contactsAdapter.notifyItemChanged(userList.indexOf(it))
                } else {
                    contactsAdapter.notifyItemChanged(groupList.indexOf(it))
                }

                rvSelected.visibility = View.VISIBLE
                fabForward.visibility = View.VISIBLE
            }
        }

        rvContacts.apply {
            itemAnimator = null
            adapter = contactsAdapter
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        }
    }

    private fun setUpSelectedAdapter() = with(binding) {
        selectedAdapter = SelectedContactsAdapter(
            context
        ) {
            selectedChats.remove(it)
            selectedAdapter.submitList(selectedChats.toMutableList())

            it.isSelected = false
            contactsAdapter.notifyItemChanged(userList.indexOf(it))

            if (selectedChats.isEmpty()) {
                fabForward.visibility = View.GONE
            }
        }

        rvSelected.apply {
            itemAnimator = null
            adapter = selectedAdapter
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        }
    }

    private fun initializeLists() = with(binding) {
        viewModel.getUserAndPhoneUser(localId).observe(viewLifecycleOwner) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData != null) {

                        userList = Tools.transformPrivateList(it.responseData)
                            .sortPrivateGroupChats(context)

                        pbForward.visibility = View.GONE
                        llForward.visibility = View.VISIBLE
                        contactsAdapter.submitList(userList.toMutableList())

//                        viewModel.getRecentContacts()
//                            .observe(viewLifecycleOwner) { recentMessages ->
//                                when (recentMessages.status) {
//                                    Resource.Status.SUCCESS -> {
//                                        if (!recentMessages.responseData.isNullOrEmpty()) {
//                                            recentContacts = recentMessages.responseData
//                                                .filter { user -> user.roomWithUsers.room.type == Const.JsonFields.PRIVATE }
//
//                                            pbForward.visibility = View.GONE
//                                            llForward.visibility = View.VISIBLE
//
//                                            setUpContactsAdapter()
//                                        } else {
//                                            contactsAdapter.submitList(userList.toMutableList())
//                                        }
//                                    }
//
//                                    else -> Timber.d("Other error")
//                                }
//                            }
                    }
                }

                Resource.Status.LOADING -> {
                    pbForward.visibility = View.VISIBLE
                    llForward.visibility = View.GONE
                }

                Resource.Status.ERROR -> {
                    // TODO ask Matko - Maybe toast with "Something went wrong" and dismiss it
                    dismiss()
                }

                else -> Timber.d("Other error::: ${it.status}, ${it.responseData}")
            }
        }

        viewModel.getAllGroups().observe(viewLifecycleOwner) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (!it.responseData.isNullOrEmpty()) {

                        groupList = Tools.transformGroupList(it.responseData)

//                        viewModel.getRecentGroups().observe(viewLifecycleOwner) { recentGroups ->
//                            when (recentGroups.status) {
//                                Resource.Status.SUCCESS -> {
//                                    if (!recentGroups.responseData.isNullOrEmpty()) {
//                                        // TODO add recent chats
//                                    } else {
//                                        // TODO
//                                    }
//                                }
//
//                                else -> Timber.d("Other error")
//                            }
//                        }
                    }
                }

                else -> {
                    Timber.d("Other error")
                }
            }
        }
    }

    private fun initializeViews() = with(binding) {
        btnContacts.setOnClickListener {
            btnContacts.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getFourthAdditionalColor(context))
            btnGroups.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getPrimaryColor(context))

            contactsAdapter.submitList(userList)
        }


        btnGroups.setOnClickListener {
            btnGroups.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getFourthAdditionalColor(context))
            btnContacts.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getPrimaryColor(context))

            showGroups()
        }

        fabForward.setOnClickListener {
            val userIds = arrayListOf<Int>()
            val roomIds = arrayListOf(0)

            selectedChats.forEach {
                userIds.add(it.id)
//                roomIds.add()
            }

            listener?.forward(userIds, roomIds)
            dismiss()
        }
    }

    private fun showGroups() {
        val list: MutableList<UserAndPhoneUser> = mutableListOf()
        Timber.d("Group")

//        groupList.forEach {
//            val element = UserAndPhoneUser(
//                User(
//                    id = it.roomWithUsers.room.roomId,
//                    displayName = it.roomWithUsers.room.name.toString(),
//                    telephoneNumber = null,
//                    avatarFileId = it.roomWithUsers.room.avatarFileId,
//                    createdAt = it.roomWithUsers.room.createdAt.toString(),
//                    modifiedAt = it.roomWithUsers.room.modifiedAt,
//                    deleted = false,
//                    emailAddress = null,
//                    telephoneNumberHashed = null,
//                    selected = false
//                ),
//                phoneUser = PhoneUser(
//                    name = it.roomWithUsers.room.name.toString(),
//                    number = "0"
//                )
//            )
//            list.add(element)
//        }

        Timber.d("List: $list")

        contactsAdapter.submitList(groupList)
    }

    private fun setUpContactsAdapter() = with(binding) {
//        val list: List<UserAndPhoneUser> = recentContacts
//            .flatMap { it.roomWithUsers.users }
//            .mapNotNull { user ->
//                userList.find { it.user.id == user.id }
//            }
//
//        list.forEach {
//            it.user.isForwarded = true
//        }
//
//        userList = list + userList

        contactsAdapter.submitList(userList.toMutableList())
    }
}
