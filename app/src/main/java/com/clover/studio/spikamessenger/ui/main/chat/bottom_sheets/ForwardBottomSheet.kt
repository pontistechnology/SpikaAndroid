package com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.entity.PrivateGroupChats
import com.clover.studio.spikamessenger.databinding.BottomSheetForwardBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.ui.main.contacts.UsersGroupsAdapter
import com.clover.studio.spikamessenger.ui.main.create_room.UsersGroupsSelectedAdapter
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.helpers.ColorHelper
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

    private lateinit var contactsAdapter: UsersGroupsAdapter
    private lateinit var selectedAdapter: UsersGroupsSelectedAdapter

    private var groupList: List<PrivateGroupChats> = mutableListOf()
    private var recentGroups: List<PrivateGroupChats> = mutableListOf()
    private var userList: List<PrivateGroupChats> = mutableListOf()
    private var recentContacts: List<PrivateGroupChats> = mutableListOf()
    private var selectedChats: MutableList<PrivateGroupChats> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetForwardBinding.inflate(layoutInflater)

        viewModel.getUserAndPhoneUser(localId)
        viewModel.getAllGroups()

        setUpAdapter()
        setUpSelectedAdapter()
        initializeObservers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setUpSearch()
    }

    private fun setUpSearch() = with(binding) {
        val list = mutableListOf<PrivateGroupChats>()
        svRoomsContacts.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    if (contactsAdapter.currentList == userList) {
                        userList.forEach {
                            if ((it.private!!.user.displayName?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true)
//                                || (it.private.phoneUser?.name?.lowercase()
//                                    ?.contains(query, ignoreCase = true) == true)
                            ) {
                                list.add(it)
                                Timber.d("Added")
                            }
                        }

                        Timber.d("list: $list, isEmpty: ${list.isEmpty()}")

                        contactsAdapter.submitList(list.toMutableList())

                    } else {
                        // Groups
                    }
                }

                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                Timber.d("New text: $query")

                if (query != null) {
                    if (contactsAdapter.currentList == userList) {
                        userList.forEach {
                            if ((it.private!!.user.displayName?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true) ||
                                (it.private.phoneUser?.name?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true)
                            ) {
                                list.add(it)
                                Timber.d("list: $list, add: $it")
                            }
                        }

                        Timber.d("list: $list, isEmpty: ${list.isEmpty()}")

                        contactsAdapter.submitList(list.toMutableList())

                    } else {
                        // Groups
                    }
                }
                return true
            }
        })
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
        contactsAdapter = UsersGroupsAdapter(
            context = requireContext(),
            isGroupCreation = false,
            userIdsInRoom = null,
            isForward = true
        )
        {
            if (!selectedChats.contains(it)) {
                selectedChats.add(it)
                selectedAdapter.submitList(selectedChats.toMutableList())

                if (contactsAdapter.currentList == userList) {
                    it.private!!.user.selected = true
                    contactsAdapter.notifyItemChanged(userList.indexOf(it))
                } else {
                    it.group!!.room.selected = true
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
        selectedAdapter = UsersGroupsSelectedAdapter(
            context
        ) {
            selectedChats.remove(it)
            selectedAdapter.submitList(selectedChats.toMutableList())

            contactsAdapter.notifyItemChanged(userList.indexOf(it))

            if (contactsAdapter.currentList == userList) {
                it.private!!.user.selected = false
                contactsAdapter.notifyItemChanged(userList.indexOf(it))
            } else {
                it.group!!.room.selected = false
                contactsAdapter.notifyItemChanged(groupList.indexOf(it))
            }

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

    private fun initializeObservers() = with(binding) {
        // TODO
        viewModel.contactListener.observe(viewLifecycleOwner, EventObserver {
            Timber.d("IT:::: ${it.status}, ${it.responseData}")
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData != null) {
                        Timber.d("Here, ${it.responseData}")

                        pbForward.visibility = View.GONE
                        llForward.visibility = View.VISIBLE

                        userList = Tools.transformPrivateList(context, it.responseData)
                        contactsAdapter.submitList(userList)

                        // TODO recent contacts just call here viewmodel
                        viewModel.getRecentContacts()
                    }
                }

                Resource.Status.LOADING -> {
                    pbForward.visibility = View.VISIBLE
                    llForward.visibility = View.GONE
                }

                Resource.Status.ERROR -> {
                    dismiss()
                }

                else -> dismiss()
            }
        })

        viewModel.recentContacts.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    Timber.d("It:::::::::: ${it.responseData}")
                    if (!it.responseData.isNullOrEmpty()) {
                        val list = Tools.transformRecentContacts(it.responseData, localId)

                    }
                }
            }
        })

        viewModel.allGroupsListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (!it.responseData.isNullOrEmpty()) {
                        groupList = Tools.transformGroupList(context, it.responseData)
                        viewModel.getRecentGroups()
                    }
                }

                else -> {
                    Timber.d("Error 2")
                }
            }
        })

        viewModel.recentGroupsListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData != null) {
                        val list = Tools.transformGroupList(context, it.responseData)
                        list.map { it1 -> it1.group!!.room.isForwarded = true }
                        groupList = list + groupList
                    }
                }

                else -> Timber.d("Other error")
            }
        })
    }

    private fun initializeViews() = with(binding) {
        btnContacts.setOnClickListener {
            btnContacts.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getPrimaryColor(context))
            btnGroups.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getFourthAdditionalColor(context))

            contactsAdapter.submitList(userList)
        }


        btnGroups.setOnClickListener {
            btnGroups.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getPrimaryColor(context))
            btnContacts.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getFourthAdditionalColor(context))

            contactsAdapter.submitList(groupList)
        }

        fabForward.setOnClickListener {
            val userIds = arrayListOf<Int>()
            val roomIds = arrayListOf<Int>()

            selectedChats.forEach {
                if (it.private != null) {
                    userIds.add(it.private.user.id)
                } else {
                    roomIds.add(it.group!!.room.roomId)
                }
            }

            Timber.d("User ids: $userIds")
            Timber.d("Group ids: $roomIds")

            // TODO for each userIds we need to find room

            listener?.forward(userIds, roomIds)
            dismiss()
        }
    }

//    private fun setUpRecentGroups(recentGroups: List<PrivateGroupChats>) {
//        recentGroups.map { it.group!!.room.isForwarded = true }
////        Timber.d("Recent groups: $recentGroups")
//        groupList = recentGroups + groupList
//    }
}
