package com.clover.studio.exampleapp.ui.main.chat_details.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.clover.studio.exampleapp.databinding.FragmentNotesDetailsBinding

class NotesDetailsFragment : Fragment() {
    private var bindingSetup: FragmentNotesDetailsBinding? = null
    private val binding get() = bindingSetup!!
    private val args: NotesDetailsFragmentArgs by navArgs()
    private var notes: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notes = args.notesDetails
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentNotesDetailsBinding.inflate(inflater, container, false)

        initializeViews()
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun initializeViews() {
        binding.tvNotesDetails.text = notes
    }
}