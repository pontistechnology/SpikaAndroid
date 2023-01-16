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
import com.clover.studio.exampleapp.data.models.entity.UserAndPhoneUser
import com.clover.studio.exampleapp.databinding.FragmentContactsBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.ui.main.UsersError
import com.clover.studio.exampleapp.ui.main.UsersFetched
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.helpers.Extensions.sortUsersByLocale
import timber.log.Timber

class ContactsFragment : BaseFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var userList: MutableList<UserAndPhoneUser>
    private var filteredList: MutableList<UserAndPhoneUser> = ArrayList()

    private var bindingSetup: FragmentContactsBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentContactsBinding.inflate(inflater, container, false)

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

        viewModel.getUserAndPhoneUser().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                userList = it.toMutableList()

                // TODO fix this later
                val users = userList.sortUsersByLocale(requireContext())

                contactsAdapter.submitList(users)
            }
        }
    }

    private fun setupAdapter() {
        contactsAdapter = ContactsAdapter(requireContext(), false, null) {
            val bundle = bundleOf(Const.Navigation.USER_PROFILE to it.user)
            findNavController().navigate(R.id.action_mainFragment_to_contactDetailsFragment, bundle)
        }

        binding.rvContacts.adapter = contactsAdapter
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvContacts.layoutManager = layoutManager
    }

    private fun setupSearchView() {
        // SearchView is immediately acting as if selected
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

}