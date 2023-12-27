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
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortPrivateChats
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
            if (!selectedChats.any { user -> user.userId == it.userId }
                && !selectedChats.any { room -> room.roomId == it.roomId }) {
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
        if (contactsAdapter.currentList == userList) {
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

                        userList = Tools.transformPrivateList(
                            context,
                            it.responseData.filter { it1 -> !it1.user.isBot })

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
                        groupList = Tools.transformGroupList(it.responseData)
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
                        val list = Tools.transformGroupList(it.responseData)
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
                if (it.phoneNumber != null && it.roomId != null) {
                    // This is contact from recent list
                    // For it we don't need to make new group, just send roomId
                    roomIds.add(it.roomId)
                } else if (it.phoneNumber != null) {
                    // This is contact selected from list
                    userIds.add(it.userId)
                } else {
                    // This is group
                    it.roomId?.let { roomId -> roomIds.add(roomId) }
                }
            }

            Timber.d("User ids: $userIds")
            Timber.d("Group ids: $roomIds")

            // TODO for each userIds we need to find room

            listener?.forward(userIds, roomIds)
            dismiss()
        }
    }

    private fun setUpSearch() = with(binding) {
        val filteredList = mutableListOf<PrivateGroupChats>()
        svRoomsContacts.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    if (contactsAdapter.currentList == userList) {
                        userList.forEach {
                            if (it.userName?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true
                            ) {
                                filteredList.add(it)
                            }
                        }
                        val users = filteredList.sortPrivateChats(requireContext())
                        contactsAdapter.submitList(ArrayList(users))
                        filteredList.clear()
                    } else {
                        groupList.forEach {
                            if (it.roomName?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true
                            ) {
                                filteredList.add(it)
                            }
                        }
                        contactsAdapter.submitList(ArrayList(filteredList))
                    }
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    Timber.d("Contacts adappter: ${contactsAdapter.currentList}")
                    if (contactsAdapter.currentList.all { it.phoneNumber != null }) {
                        userList.forEach {
                            if (it.userName?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true
                            ) {
                                filteredList.add(it)
                            }
                        }
                        Timber.d("Here 1")
                    } else {
                        groupList.forEach {
                            if (it.roomName?.lowercase()
                                    ?.contains(query, ignoreCase = true) == true
                            ) {
                                filteredList.add(it)
                            }
                        }
                        Timber.d("Here 2")
                    }

                    val users = filteredList.sortPrivateChats(requireContext())
                    contactsAdapter.submitList(ArrayList(users))
                    filteredList.clear()

                } else {
                    contactsAdapter.submitList(userList)
                }
                return true
            }
        })
    }
}
