package com.clover.studio.spikamessenger.ui.main.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.data.models.entity.UserAndPhoneUser
import com.clover.studio.spikamessenger.databinding.FragmentContactsBinding
import com.clover.studio.spikamessenger.ui.main.MainViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.helpers.Extensions.sortUsersByLocale
import com.clover.studio.spikamessenger.utils.helpers.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ContactsFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var userList: MutableList<UserAndPhoneUser>
    private var filteredList: MutableList<UserAndPhoneUser> = ArrayList()
    private var selectedUser: User? = null

    private var bindingSetup: FragmentContactsBinding? = null
    private val binding get() = bindingSetup!!

    private var navOptionsBuilder: NavOptions? = null

    private var localId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentContactsBinding.inflate(inflater, container, false)
        navOptionsBuilder = Tools.createCustomNavOptions()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        localId = viewModel.getLocalUserId()!!
        setupAdapter()
        setupSwipeToRefresh()
        initializeViews()
        initializeObservers()
    }

    private fun setupSwipeToRefresh() {
        binding.srlRefreshContacts.setOnRefreshListener {
            viewModel.syncContacts()
        }
        binding.srlRefreshContacts.isEnabled = true
    }

    private fun initializeViews() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search_menu_icon -> {
                    val searchView = menuItem.actionView as SearchView
                    searchView.let { Tools.setUpSearchBar(requireContext(), it) }
                    setupSearchView(searchView)

                    menuItem.expandActionView()

                    true
                }

                else -> false
            }
        }
    }

    private fun initializeObservers() {
        viewModel.usersListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> Timber.d("Users fetched")
                Resource.Status.ERROR -> Timber.d("Users error")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getUserAndPhoneUser(localId).observe(viewLifecycleOwner) {
            if (it.responseData != null) {
                userList = it.responseData.toMutableList()

                // TODO fix this later
                val users = userList.sortUsersByLocale(requireContext())

                contactsAdapter.submitList(users)
            }
        }

        // Listener will check if the selected user has a room open with you. If he does
        // the method will send the user to the room with the roomId which is required to
        // handle mute and pin logic. If the user has no room open the roomId will be 0
        viewModel.checkRoomExistsListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (selectedUser != null) {
                        val bundle = bundleOf(
                            Const.Navigation.USER_PROFILE to selectedUser,
                            Const.Navigation.ROOM_ID to it.responseData?.data?.room?.roomId,
                            Const.Navigation.ROOM_DATA to it.responseData?.data?.room
                        )
                        findNavController().navigate(
                            R.id.action_mainFragment_to_contactDetailsFragment,
                            bundle,
                            navOptionsBuilder
                        )
                    }
                }

                Resource.Status.ERROR -> {
                    if (selectedUser != null) {
                        val bundle = bundleOf(Const.Navigation.USER_PROFILE to selectedUser)
                        findNavController().navigate(
                            R.id.action_mainFragment_to_contactDetailsFragment,
                            bundle,
                            navOptionsBuilder
                        )
                    }
                }

                else -> Timber.d("Other error")
            }
        })

        viewModel.contactSyncListener.observe(viewLifecycleOwner, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    Timber.d("Contact sync success")
                    binding.srlRefreshContacts.isRefreshing = false
                }

                Resource.Status.ERROR -> {
                    Timber.d("Contact sync error")
                    Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_SHORT).show()
                    binding.srlRefreshContacts.isRefreshing = false
                }

                else -> {
                    Timber.d("Contact sync else")
                    Toast.makeText(activity, getString(R.string.error), Toast.LENGTH_SHORT).show()
                    binding.srlRefreshContacts.isRefreshing = false
                }
            }
        })
    }

    private fun setupAdapter() {
        contactsAdapter = ContactsAdapter(requireContext(), false, null, isForward = false) {
            selectedUser = it.user
            run {
                CoroutineScope(Dispatchers.IO).launch {
                    Timber.d("Checking room id: ${viewModel.checkIfUserInPrivateRoom(it.user.id)}")
                    val roomId = viewModel.checkIfUserInPrivateRoom(it.user.id)
                    if (roomId != null) {
                        if (selectedUser != null) {
                            val bundle = bundleOf(
                                Const.Navigation.USER_PROFILE to selectedUser,
                                Const.Navigation.ROOM_ID to roomId
                            )
                            activity?.runOnUiThread {
                                findNavController().navigate(
                                    R.id.action_mainFragment_to_contactDetailsFragment,
                                    bundle,
                                    navOptionsBuilder
                                )
                            }
                        }
                    } else {
                        viewModel.checkIfRoomExists(it.user.id)
                    }
                }
            }
        }

        binding.rvContacts.adapter = contactsAdapter
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvContacts.layoutManager = layoutManager
    }

    private fun setupSearchView(searchView: SearchView) {
        // SearchView is immediately acting as if selected
        searchView.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    if (::userList.isInitialized) {
                        for (user in userList) {
                            if (user.phoneUser?.name?.lowercase()?.contains(
                                    query,
                                    ignoreCase = true
                                ) ?: user.user.formattedDisplayName.lowercase()
                                    .contains(query, ignoreCase = true)
                            ) {
                                filteredList.add(user)
                            }
                        }
                        val users = filteredList.sortUsersByLocale(requireContext())
                        contactsAdapter.submitList(ArrayList(users))
                        filteredList.clear()
                    }
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    if (::userList.isInitialized) {
                        for (user in userList) {
                            if (user.phoneUser?.name?.lowercase()?.contains(
                                    query,
                                    ignoreCase = true
                                ) ?: user.user.formattedDisplayName.lowercase()
                                    .contains(query, ignoreCase = true)
                            ) {
                                filteredList.add(user)
                            }
                        }
                        val users = filteredList.sortUsersByLocale(requireContext())
                        contactsAdapter.submitList(ArrayList(users))
                        filteredList.clear()
                    }
                }
                return true
            }
        })

        searchView.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.topAppBar.menu.findItem(R.id.search_menu_icon).collapseActionView()
    }
}
