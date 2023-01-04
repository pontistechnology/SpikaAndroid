package com.clover.studio.exampleapp.ui.main.chat_details.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.clover.studio.exampleapp.databinding.FragmentNewNoteBinding

class NewNoteFragment : Fragment() {
    private var bindingSetup: FragmentNewNoteBinding? = null
    private val binding get() = bindingSetup!!
    private val args: NewNoteFragmentArgs by navArgs()
    private var roomId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = args.roomId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentNewNoteBinding.inflate(inflater, container, false)

        initializeViews()
        return binding.root
    }

    private fun initializeViews() {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressed()
        }
    }
}