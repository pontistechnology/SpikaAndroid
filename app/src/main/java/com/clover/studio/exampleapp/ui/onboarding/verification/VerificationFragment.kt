package com.clover.studio.exampleapp.ui.onboarding.verification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.clover.studio.exampleapp.databinding.FragmentVerificationBinding

class VerificationFragment : Fragment() {
    private var bindingSetup: FragmentVerificationBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }
}