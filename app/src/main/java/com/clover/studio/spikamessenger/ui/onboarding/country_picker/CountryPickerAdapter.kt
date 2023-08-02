package com.clover.studio.spikamessenger.ui.onboarding.country_picker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.data.models.CountryCode
import com.clover.studio.spikamessenger.databinding.ItemCountryCodeBinding

class CountryPickerAdapter(private val onItemClick: ((item: CountryCode) -> Unit)) :
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
                binding.tvCountryName.text = countryItem.name
                binding.tvCountryCode.text = countryItem.dial_code

                itemView.setOnClickListener {
                    countryItem.let {
                        onItemClick.invoke(it)
                    }
                }
            }
        }
    }

    private class CountryDiffCallback : DiffUtil.ItemCallback<CountryCode>() {

        override fun areItemsTheSame(oldItem: CountryCode, newItem: CountryCode) =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: CountryCode, newItem: CountryCode) =
            oldItem == newItem
    }
}