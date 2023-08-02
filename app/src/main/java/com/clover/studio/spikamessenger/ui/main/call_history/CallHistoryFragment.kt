package com.clover.studio.spikamessenger.ui.main.call_history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.entity.User
import com.clover.studio.spikamessenger.databinding.FragmentCallHistoryBinding
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
        setupSearchView()

        if (!this::userList.isInitialized || userList.isEmpty()) {
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

        if (this::userList.isInitialized && userList.isNotEmpty()) {
            callHistoryAdapter.submitList(
                userList
            )
        }
    }

    private fun setupSearchView() {
        // SearchView is immediately acting as if selected
        if (this::userList.isInitialized) {
            binding.svHistorySearch.setIconifiedByDefault(false)
            binding.svHistorySearch.setOnQueryTextListener(object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (query != null) {
                        Timber.d("Query: $query")
                        for (user in userList) {
                            if (user.formattedDisplayName?.contains(
                                    query,
                                    ignoreCase = true
                                ) == true
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
                            if (user.formattedDisplayName?.contains(
                                    query,
                                    ignoreCase = true
                                ) == true
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
        binding.svHistorySearch.setOnFocusChangeListener { view, hasFocus ->
            run {
                view.clearFocus()
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
        }
    }
}