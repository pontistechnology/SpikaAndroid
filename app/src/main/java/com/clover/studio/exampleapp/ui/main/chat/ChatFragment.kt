package com.clover.studio.exampleapp.ui.main.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.clover.studio.exampleapp.databinding.FragmentChatBinding
import com.clover.studio.exampleapp.utils.extendables.BaseFragment

class ChatFragment : BaseFragment() {
    private var bindingSetup: FragmentChatBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }
}