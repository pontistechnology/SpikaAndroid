package com.clover.studio.exampleapp.ui.main.call_history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.clover.studio.exampleapp.databinding.FragmentCallHistoryBinding

class CallHistoryFragment : Fragment() {
    private var bindingSetup: FragmentCallHistoryBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentCallHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }
}