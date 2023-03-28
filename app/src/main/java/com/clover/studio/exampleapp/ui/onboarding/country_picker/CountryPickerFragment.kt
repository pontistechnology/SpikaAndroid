package com.clover.studio.exampleapp.ui.onboarding.country_picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.CountryCode
import com.clover.studio.exampleapp.databinding.FragmentCountryPickerBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.extendables.BaseFragment
import com.clover.studio.exampleapp.utils.helpers.GsonProvider
import com.google.gson.Gson
import timber.log.Timber


class CountryPickerFragment : BaseFragment() {
    private lateinit var countryPickerAdapter: CountryPickerAdapter
    private lateinit var countryList: List<CountryCode>
    private var filteredList: MutableList<CountryCode> = ArrayList()

    private var bindingSetup: FragmentCountryPickerBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        bindingSetup = FragmentCountryPickerBinding.inflate(inflater, container, false)

        setupSearchView()
        setupAdapter()

        binding.tvCancel.setOnClickListener {
            findNavController().navigate(
                R.id.action_countryPickerFragment_to_splashFragment
            )
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingSetup = null
    }

    private fun setupAdapter() {
        countryPickerAdapter = CountryPickerAdapter {
            val bundle = bundleOf(
                Const.Navigation.COUNTRY_CODE to it.dial_code
            )
            findNavController().navigate(
                R.id.action_countryPickerFragment_to_splashFragment,
                bundle
            )
        }
        binding.rvCountryCodes.adapter = countryPickerAdapter
        binding.rvCountryCodes.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        // Load local json list
        val list =
            resources.openRawResource(R.raw.country_codes).bufferedReader().use { it.readText() }

        val gson = GsonProvider.gson
        countryList = gson.fromJson(list, Array<CountryCode>::class.java).toList()
        countryPickerAdapter.submitList(
            countryList
        )
    }

    private fun setupSearchView() {
        // SearchView is immediately acting as if selected
        binding.svCountrySearch.setIconifiedByDefault(false)
        binding.svCountrySearch.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    Timber.d("Query: $query")
                    for (country in countryList) {
                        if (country.name.contains(query, ignoreCase = true)) {
                            filteredList.add(country)
                        }
                    }
                    Timber.d("Filtered List: $filteredList")
                    countryPickerAdapter.submitList(ArrayList(filteredList))
                    filteredList.clear()
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                if (query != null) {
                    Timber.d("Query: $query")
                    for (country in countryList) {
                        if (country.name.contains(query, ignoreCase = true)) {
                            filteredList.add(country)
                        }
                    }
                    Timber.d("Filtered List: $filteredList")
                    countryPickerAdapter.submitList(ArrayList(filteredList))
                    filteredList.clear()
                }
                return true
            }
        })
    }
}