package com.clover.studio.exampleapp.ui.onboarding.country_picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.CountryCode
import com.clover.studio.exampleapp.databinding.FragmentCountryPickerBinding
import com.clover.studio.exampleapp.utils.Const

class CountryPickerFragment : Fragment() {
    private lateinit var countryPickerAdapter: CountryPickerAdapter

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

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingSetup = null
    }

    private fun setupAdapter() {
        countryPickerAdapter = CountryPickerAdapter { position, item ->
            val bundle = bundleOf(
                Const.Navigation.COUNTRY_CODE to item.countryCode
            )
            findNavController().navigate(
                R.id.action_countryPickerFragment_to_splashFragment,
                bundle
            )
        }
        binding.rvCountryCodes.adapter = countryPickerAdapter
        binding.rvCountryCodes.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        // Mock item
        val itemList = listOf<CountryCode>(CountryCode("Croatia", "+41"))
        countryPickerAdapter.submitList(itemList)
    }

    private fun setupSearchView() {
        // SearchView is immediately acting as if selected
        binding.svCountrySearch.setIconifiedByDefault(false)
        binding.svCountrySearch.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // TODO filter list and submit
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                // TODO filter list and submit
                return true
            }

        })
    }

    override fun onResume() {
        super.onResume()
        // TODO fetch list and populate adapter
    }
}