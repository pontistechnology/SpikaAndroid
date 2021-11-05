package com.clover.studio.exampleapp.ui.onboarding.country_picker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.data.models.CountryCode
import com.clover.studio.exampleapp.databinding.ItemCountryCodeBinding

class CountryPickerAdapter(private val onItemClick: ((position: Int, item: CountryCode) -> Unit)) :
    ListAdapter<CountryCode, CountryPickerAdapter.CountryPickerViewHolder>(CountryDiffCallback()) {

    inner class CountryPickerViewHolder(val binding: ItemCountryCodeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryPickerViewHolder {
        val binding =
            ItemCountryCodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CountryPickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountryPickerViewHolder, position: Int) {
        with(holder) {
            getItem(position).let { countryItem ->
                binding.tvCountryName.text = countryItem.countryName
                binding.tvCountryCode.text = countryItem.countryCode

                itemView.setOnClickListener {
                    countryItem.let {
                        onItemClick.invoke(adapterPosition, it)
                    }
                }
            }
        }
    }

    private class CountryDiffCallback : DiffUtil.ItemCallback<CountryCode>() {

        override fun areItemsTheSame(oldItem: CountryCode, newItem: CountryCode) =
            oldItem.countryName == newItem.countryName

        override fun areContentsTheSame(oldItem: CountryCode, newItem: CountryCode) =
            oldItem == newItem
    }
}