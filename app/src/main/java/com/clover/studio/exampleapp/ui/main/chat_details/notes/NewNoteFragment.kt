package com.clover.studio.exampleapp.ui.main.chat_details.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.networking.NewNote
import com.clover.studio.exampleapp.databinding.FragmentNewNoteBinding
import com.clover.studio.exampleapp.ui.main.chat.ChatStates
import com.clover.studio.exampleapp.ui.main.chat.ChatViewModel
import com.clover.studio.exampleapp.ui.main.chat.NoteCreated
import com.clover.studio.exampleapp.ui.main.chat.NoteCreationFailed
import com.clover.studio.exampleapp.utils.EventObserver
import timber.log.Timber
import androidx.navigation.fragment.navArgs

class NewNoteFragment : Fragment() {
    private var bindingSetup: FragmentNewNoteBinding? = null
    private val binding get() = bindingSetup!!

    private val viewModel: ChatViewModel by activityViewModels()
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
        initializeObservers()
        return binding.root
    }

    private fun initializeObservers() {
        viewModel.noteCreationListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                NoteCreated -> activity?.onBackPressed()
                NoteCreationFailed -> Toast.makeText(context, getString(R.string.note_creation_failed), Toast.LENGTH_SHORT)
                else -> Timber.d("Other error")
            }
        })
    }

    private fun initializeViews() {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.tvSave.setOnClickListener {
            if (binding.etTitle.text.isNotEmpty() && binding.etDescription.text.isNotEmpty()) {
                val newNote =
                    NewNote(binding.etTitle.text.toString(), binding.etDescription.text.toString())
                viewModel.createNewNote(roomId, newNote)
            } else {
                Toast.makeText(context, getString(R.string.note_error_toast), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}