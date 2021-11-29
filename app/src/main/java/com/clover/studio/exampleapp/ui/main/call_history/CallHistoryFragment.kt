package com.clover.studio.exampleapp.ui.main.call_history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.FragmentCallHistoryBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import timber.log.Timber

class CallHistoryFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var callHistoryAdapter: CallHistoryAdapter
    private lateinit var userList: List<User>
    private var filteredList: MutableList<User> = ArrayList()

    private var bindingSetup: FragmentCallHistoryBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentCallHistoryBinding.inflate(inflater, container, false)

        // TODO get user list and check state
        userList = mutableListOf(
            User(
                1,
                "mojemail@lol.com",
                "+384945556666",
                "somehash",
                "+385",
                "Matom",
                "someUrl",
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
                "someUrl",
                "Time",
                "time"
            )
        )

        setupAdapter()
        setupSearchView()

        if (userList.isEmpty()) {
            binding.svHistorySearch.visibility = View.GONE
            binding.rvCallHistory.visibility = View.GONE
        } else {
            binding.tvNoCalls.visibility = View.GONE
        }

        return binding.root
    }

    private fun setupAdapter() {
        callHistoryAdapter = CallHistoryAdapter(requireContext()) {
            // TODO handle navigation when item clicked
        }

        binding.rvCallHistory.adapter = callHistoryAdapter
        binding.rvCallHistory.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        callHistoryAdapter.submitList(
            userList
        )
    }

    private fun setupSearchView() {
        // SearchView is immediately acting as if selected
        binding.svHistorySearch.setIconifiedByDefault(false)
        binding.svHistorySearch.setOnQueryTextListener(object :
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
                    callHistoryAdapter.submitList(ArrayList(filteredList))
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
                    callHistoryAdapter.submitList(ArrayList(filteredList))
                    filteredList.clear()
                }
                return true
            }
        })
    }
}