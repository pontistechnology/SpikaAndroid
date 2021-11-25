package com.clover.studio.exampleapp.ui.main.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.FragmentContactsBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.utils.Const
import timber.log.Timber

class ContactsFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var userList: List<User>
    private var filteredList: MutableList<User> = ArrayList()

    private var bindingSetup: FragmentContactsBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentContactsBinding.inflate(inflater, container, false)

        // TODO get user list and check state
        userList = mutableListOf(
            User(
                "1",
                "Matko T.",
                "MaTom",
                "someUrl",
                "localname",
                false,
                "Time",
                "Time"
            ),

            User(
                "2",
                "Marko M.",
                "Marko",
                "someUrl",
                "localname",
                false,
                "Time",
                "Time"
            ),

            User(
                "3",
                "Ivan L.",
                "Ivankovic",
                "someUrl",
                "localname",
                false,
                "Time",
                "Time"
            ),

            User(
                "4",
                "Zdravko M.",
                "Zdravkic",
                "someUrl",
                "localname",
                false,
                "Time",
                "Time"
            )
        )

        setupAdapter()
        setupSearchView()

        return binding.root
    }

    private fun setupAdapter() {
        contactsAdapter = ContactsAdapter(requireContext()) {
            val bundle = bundleOf(Const.Navigation.USER_PROFILE to it)

            findNavController().navigate(R.id.action_mainFragment_to_contactDetailsFragment, bundle)
        }

        binding.rvContacts.adapter = contactsAdapter
        binding.rvContacts.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        contactsAdapter.submitList(
            userList
        )
    }

    private fun setupSearchView() {
        // SearchView is immediately acting as if selected
        binding.svContactsSearch.setIconifiedByDefault(false)
        binding.svContactsSearch.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    Timber.d("Query: $query")
                    for (user in userList) {
                        if (user.nickname.contains(query, ignoreCase = true)) {
                            filteredList.add(user)
                        }
                    }
                    Timber.d("Filtered List: $filteredList")
                    contactsAdapter.submitList(ArrayList(filteredList))
                    filteredList.clear()
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    Timber.d("Query: $query")
                    for (user in userList) {
                        if (user.nickname.contains(query, ignoreCase = true)) {
                            filteredList.add(user)
                        }
                    }
                    Timber.d("Filtered List: $filteredList")
                    contactsAdapter.submitList(ArrayList(filteredList))
                    filteredList.clear()
                }
                return true
            }
        })
    }
}