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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.utils.extendables.BaseFragment

class NotesFragment : BaseFragment() {
    private var bindingSetup: FragmentNotesBinding? = null
    private val viewModel: ChatViewModel by activityViewModels()
    private val binding get() = bindingSetup!!
    private lateinit var adapter: NotesAdapter
    private val args: NotesFragmentArgs by navArgs()
    private var roomId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = args.roomId

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentNotesBinding.inflate(inflater, container, false)
        setupAdapter()
        initializeViews()
        initializeObservers()
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun initializeViews() {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun initializeObservers() {
        viewModel.getRoomNotes(roomId).observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        viewModel.fetchNotes(roomId)
    }

    private fun setupAdapter() {
        adapter = NotesAdapter(requireActivity()) {
            val action = it.content?.let { content ->
                NotesFragmentDirections.actionNotesFragmentToNotesDetailsFragment(
                    content, it.title!!
                )
            }

            if (action != null) {
                findNavController().navigate(action)
            }
        }

        binding.rvNotes.itemAnimator = null
        binding.rvNotes.adapter = adapter
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        binding.rvNotes.layoutManager = layoutManager
    }

    override fun onDestroy() {
        super.onDestroy()
        bindingSetup = null
    }
}