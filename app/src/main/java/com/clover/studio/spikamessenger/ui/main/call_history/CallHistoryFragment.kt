package com.clover.studio.spikamessenger.ui.main.call_history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.databinding.FragmentCallHistoryBinding
import com.clover.studio.spikamessenger.ui.main.MainFragmentDirections
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import timber.log.Timber

class CallHistoryFragment : BaseFragment() {
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

        setupAdapter()
        initializeViews()

        if (!this::userList.isInitialized || userList.isEmpty()) {
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

        if (this::userList.isInitialized && userList.isNotEmpty()) {
            callHistoryAdapter.submitList(
                userList
            )
        }
    }

    private fun initializeViews() = with(binding){
        topAppBar.menu.findItem(R.id.create_room_menu_icon).isVisible = false
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search_menu_icon -> {
                    val searchView = menuItem.actionView as SearchView
                    searchView.queryHint = getString(R.string.call_history_search_hint)
                    searchView.setIconifiedByDefault(false)
                    setupSearchView(searchView)

                    menuItem.expandActionView()

                    true
                }

                R.id.create_room_menu_icon -> {
                    findNavController().navigate(MainFragmentDirections.actionMainFragmentToNewRoomFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun setupSearchView(searchView: SearchView) {
        // SearchView is immediately acting as if selected
        if (this::userList.isInitialized) {
            searchView.setIconifiedByDefault(false)
            searchView.setOnQueryTextListener(object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query != null) {
                        Timber.d("Query: $query")
                        for (user in userList) {
                            if (user.formattedDisplayName.contains(
                                    query,
                                    ignoreCase = true
                                )
                            ) {
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
                            if (user.formattedDisplayName.contains(
                                    query,
                                    ignoreCase = true
                                )
                            ) {
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
        searchView.setOnFocusChangeListener { view, hasFocus ->
            run {
                view.clearFocus()
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
        }
    }
}
