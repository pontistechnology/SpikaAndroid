package com.clover.studio.exampleapp.ui.main.chat_details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.utils.extendables.BaseFragment

class ChatDetailsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_details, container, false)
    }
}