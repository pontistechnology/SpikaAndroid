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
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortPrivateGroupChats
import com.clover.studio.spikamessenger.utils.helpers.Resource
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
    private var userList: List<PrivateGroupChats> = mutableListOf()
    private var selectedChats: MutableList<PrivateGroupChats> = mutableListOf()
    private var searchQuery: String = ""

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

        val bottomSheetBehavior = BottomSheetBehavior.from((view.parent as View))

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val peekHeight = (screenHeight)
        bottomSheetBehavior.peekHeight = peekHeight

        binding.rvContacts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        })
        setUpSearch()
    }

    companion object {
        const val TAG = "forwardSheet"
    }

    interface BottomSheetForwardAction {
        fun forward(userIds: ArrayList<Int>, roomIds: ArrayList<Int>)
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
            if (!selectedChats.any { item ->
                    (isPrivateUser() && item.userId == it.userId) ||
                            (!isPrivateUser() && item.roomId == it.roomId)
                }) {
                selectedChats.add(it)
                selectedAdapter.submitList(selectedChats.toMutableList())
                refreshAdapter(it, true)
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

    private fun isPrivateUser() = contactsAdapter.currentList.all { it.phoneNumber !=  null }

    private fun setUpSelectedAdapter() = with(binding) {
        selectedAdapter = UsersGroupsSelectedAdapter(
            context
        ) {
            selectedChats.remove(it)
            selectedAdapter.submitList(selectedChats.toMutableList())

            refreshAdapter(it, false)

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

    private fun refreshAdapter(list: PrivateGroupChats, isSelected: Boolean) {
        if (isPrivateUser()) {
            userList
                .filter { user -> user.userId == list.userId }
                .forEach { user ->
                    val position = userList.indexOf(user)
                    if (position != -1) {
                        user.selected = isSelected
                        contactsAdapter.notifyItemChanged(position)
                    }
                }
        } else {
            groupList
                .filter { room -> room.roomId == list.roomId }
                .forEach { room ->
                    val position = groupList.indexOf(room)
                    if (position != -1) {
                        room.selected = isSelected
                        contactsAdapter.notifyItemChanged(position)
                    }
                }
        }
    }

    private fun initializeObservers() = with(binding) {
        viewModel.contactListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData != null) {
                        pbForward.visibility = View.GONE
                        llForward.visibility = View.VISIBLE

                        userList = Tools.transformPrivateList(context, it.responseData)

                        viewModel.getRecentContacts()
                    }
                }

                else -> Timber.d("Other error")
            }
        })

        viewModel.recentContacts.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (!it.responseData.isNullOrEmpty()) {
                        val list = Tools.transformRecentContacts(localId, it.responseData)
                        list.map { it1 -> it1.isForwarded = true }
                        userList = list + userList
                        contactsAdapter.submitList(userList)
                    }
                }

                else -> contactsAdapter.submitList(userList)
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

                else -> Timber.d("Other error")
            }
        })

        viewModel.recentGroupsListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData != null) {
                        val list = Tools.transformGroupList(context, it.responseData)
                        list.map { it1 -> it1.isForwarded = true }
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

            if (searchQuery.isNotEmpty()) {
                svRoomsContacts.setQuery(searchQuery, true)
                setUpSearch()
            }
        }

        btnGroups.setOnClickListener {
            btnGroups.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getPrimaryColor(context))
            btnContacts.backgroundTintList =
                ColorStateList.valueOf(ColorHelper.getFourthAdditionalColor(context))

            contactsAdapter.submitList(groupList)

            if (searchQuery.isNotEmpty()) {
                Timber.d("Searching groups")
                svRoomsContacts.setQuery(searchQuery, true)
                setUpSearch()
            }
        }

        fabForward.setOnClickListener {
            val userIds = arrayListOf<Int>()
            val roomIds = arrayListOf<Int>()

            selectedChats.forEach {
                if (it.phoneNumber != null && it.roomId != null) {
                    // This is contact from recent list
                    // For it we don't need to make new group, just send roomId
                    roomIds.add(it.roomId)
                } else if (it.phoneNumber != null) {
                    // This is contact selected from list
                    it.userId.let { userId -> userIds.add(userId) }
                } else {
                    // This is group
                    it.roomId?.let { roomId -> roomIds.add(roomId) }
                }
            }

            listener?.forward(userIds, roomIds)
            dismiss()
        }
    }

    private var filteredList: MutableList<PrivateGroupChats> = mutableListOf()

    private fun setUpSearch() = with(binding) {
        svRoomsContacts.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    searchQuery = query
                    filteredList = if (isPrivateUser()) {
                        userList.filter {
                            it.userName?.contains(query, ignoreCase = true) == true
                        }.toMutableList()
                    } else {
                        Timber.d("Here groups: $query")
                        groupList.filter {
                            it.roomName?.contains(query, ignoreCase = true) == true
                        }.toMutableList()
                    }

                    val list = filteredList.sortPrivateGroupChats(requireContext())
                    contactsAdapter.submitList(ArrayList(list))
                    filteredList.clear()

                } else {
                    contactsAdapter.submitList(userList)
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    searchQuery = query
                    filteredList = if (isPrivateUser()) {
                        userList.filter {
                            it.userName?.contains(query, ignoreCase = true) == true
                        }.toMutableList()
                    } else {
                        Timber.d("Here groups: $query")
                        groupList.filter {
                            it.roomName?.contains(query, ignoreCase = true) == true
                        }.toMutableList()
                    }

                    val list = filteredList.sortPrivateGroupChats(requireContext())
                    contactsAdapter.submitList(ArrayList(list))
                    filteredList.clear()

                } else {
                    contactsAdapter.submitList(userList)
                }
                return true
            }
        })
    }
}
