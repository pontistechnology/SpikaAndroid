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
import com.clover.studio.exampleapp.ui.main.UsersError
import com.clover.studio.exampleapp.ui.main.UsersFetched
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.Tools
import timber.log.Timber
import kotlin.random.Random

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
                1,
                "mojemail@lol.com",
                "+384945556666",
                "somehash",
                "+385",
                "Matom",
                Tools.getRandomImageUrl(Random.nextInt(5)),
                "Time",
                "time"
            ),

            User(
                2,
                "drugimai@aol.com",
                "+384945556666",
                "somehash",
                "+041",
                "Markan",
                Tools.getRandomImageUrl(Random.nextInt(5)),
                "Time",
                "time"
            ),

            User(
                3,
                "novimail@yahoo.com",
                "+024945556666",
                "somehash",
                "+024",
                "Ivankovic",
                Tools.getRandomImageUrl(Random.nextInt(5)),
                "Time",
                "time"
            ),

            User(
                4,
                "madaj@google.com",
                "+234945556666",
                "somehash",
                "+234",
                "Zdravkic",
                Tools.getRandomImageUrl(Random.nextInt(5)),
                "Time",
                "time"
            )
        )

        setupAdapter()
        setupSearchView()
        initializeObservers()

        return binding.root
    }

    private fun initializeObservers() {
        viewModel.usersListener.observe(viewLifecycleOwner, {
            when (it) {
                UsersError -> Timber.d("Users error")
                is UsersFetched -> Timber.d("Users fetched: ${it.userData}")
            }
        })

        viewModel.getContacts(1)
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
                        if (user.displayName?.contains(query, ignoreCase = true) == true) {
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
                        if (user.displayName?.contains(query, ignoreCase = true) == true) {
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