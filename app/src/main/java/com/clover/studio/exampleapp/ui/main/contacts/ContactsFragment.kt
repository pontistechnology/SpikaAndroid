package com.clover.studio.exampleapp.ui.main.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.User
import com.clover.studio.exampleapp.data.models.entity.UserAndPhoneUser
import com.clover.studio.exampleapp.databinding.FragmentContactsBinding
import com.clover.studio.exampleapp.ui.main.*
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.helpers.Extensions.sortUsersByLocale
import com.clover.studio.exampleapp.utils.helpers.Resource
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

    private var localId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentContactsBinding.inflate(inflater, container, false)

        localId = viewModel.getLocalUserId()!!
        setupAdapter()
        setupSearchView()
        initializeObservers()

        return binding.root
    }

    private fun initializeObservers() {
        viewModel.usersListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                UsersError -> Timber.d("Users error")
                UsersFetched -> Timber.d("Users fetched")
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
                            Const.Navigation.ROOM_ID to it.responseData?.data?.room?.roomId
                        )
                        findNavController().navigate(
                            R.id.action_mainFragment_to_contactDetailsFragment,
                            bundle
                        )
                    }
                }
                Resource.Status.ERROR -> {
                    if (selectedUser != null) {
                        val bundle = bundleOf(Const.Navigation.USER_PROFILE to selectedUser)
                        findNavController().navigate(
                            R.id.action_mainFragment_to_contactDetailsFragment,
                            bundle
                        )
                    }
                }
                else -> Timber.d("Other error")
            }
        })
    }

    private fun setupAdapter() {
        contactsAdapter = ContactsAdapter(requireContext(), false, null) {
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
                                    bundle
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

    private fun setupSearchView() {
        // SearchView is immediately acting as if selected
        binding.svContactsSearch.setQuery("", false)
        binding.svContactsSearch.setIconifiedByDefault(false)
        binding.svContactsSearch.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    if (::userList.isInitialized) {
                        for (user in userList) {
                            if ((user.phoneUser?.name?.lowercase()?.contains(
                                    query,
                                    ignoreCase = true
                                ) ?: user.user.displayName?.lowercase()
                                    ?.contains(query, ignoreCase = true)) == true
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
                            if ((user.phoneUser?.name?.lowercase()?.contains(
                                    query,
                                    ignoreCase = true
                                ) ?: user.user.displayName?.lowercase()
                                    ?.contains(query, ignoreCase = true)) == true
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

        binding.svContactsSearch.setOnFocusChangeListener { view, hasFocus ->
            run {
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupSearchView()
    }

}