package com.clover.studio.exampleapp.ui.main.chat_details.notes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.FragmentNotesBinding
import com.clover.studio.exampleapp.ui.main.chat.ChatViewModel
import com.clover.studio.exampleapp.ui.main.chat.NotesFetched
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.extendables.BaseFragment

class NotesFragment : BaseFragment() {
    private var bindingSetup: FragmentNotesBinding? = null
    private val viewModel: ChatViewModel by activityViewModels()
    private val binding get() = bindingSetup!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentNotesBinding.inflate(inflater, container, false)
        setupAdapter()
        initializeObservers()
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun initializeObservers() {
        viewModel.noteDataListener.observe(viewLifecycleOwner, EventObserver {
            when(it) {
                is NotesFetched -> {
                    // TODO send data to adapter
                }
            }
        })
    }

    private fun setupAdapter() {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        bindingSetup = null
    }
}