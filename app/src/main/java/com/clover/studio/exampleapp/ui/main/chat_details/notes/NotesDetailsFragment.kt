package com.clover.studio.exampleapp.ui.main.chat_details.notes

import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.networking.NewNote
import com.clover.studio.exampleapp.databinding.FragmentNotesDetailsBinding
import com.clover.studio.exampleapp.ui.main.chat.ChatViewModel
import com.clover.studio.exampleapp.ui.main.chat.NoteCreationFailed
import com.clover.studio.exampleapp.ui.main.chat.NoteUpdated
import com.clover.studio.exampleapp.utils.EventObserver
import io.noties.markwon.LinkResolver
import org.commonmark.node.Link
import io.noties.markwon.Markwon
import timber.log.Timber

class NotesDetailsFragment : Fragment() {
    private var bindingSetup: FragmentNotesDetailsBinding? = null
    private val binding get() = bindingSetup!!
    private val viewModel: ChatViewModel by activityViewModels()
    private val args: NotesDetailsFragmentArgs by navArgs()
    private var notes: String = ""
    private var notesName: String = ""
    private var noteId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notes = args.notesDetails
        notesName = args.notesName
        noteId = args.noteId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentNotesDetailsBinding.inflate(inflater, container, false)

        initializeViews()
        initializeObservers()
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun initializeObservers() {
        viewModel.noteCreationListener.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                NoteUpdated -> {
                    binding.tvTitle.visibility = View.VISIBLE
                    binding.tvNotesDetails.visibility = View.VISIBLE
                    binding.etTitle.visibility = View.GONE
                    binding.etDescription.visibility = View.GONE

                    binding.tvEdit.text = getString(R.string.edit)

                    notesName = binding.etTitle.text.toString()
                    notes = binding.etDescription.text.toString()

                    markdownNotes()
                }
                NoteCreationFailed -> Toast.makeText(context, getString(R.string.note_creation_failed), Toast.LENGTH_SHORT)
                else -> Timber.d("Other error")
            }
        })
    }

    private fun initializeViews() {
        markdownNotes()

        binding.ivBack.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.tvNotesDetails.text = notes

        binding.tvEdit.setOnClickListener {
            if (binding.tvEdit.text == getString(R.string.save)) {
                // TODO upload updated note
                binding.tvTitle.visibility = View.VISIBLE
                binding.tvNotesDetails.visibility = View.VISIBLE
                binding.etTitle.visibility = View.GONE
                binding.etDescription.visibility = View.GONE

                binding.tvEdit.text = getString(R.string.edit)
            } else {
                binding.tvTitle.visibility = View.GONE
                binding.tvNotesDetails.visibility = View.GONE
                binding.etTitle.visibility = View.VISIBLE
                binding.etDescription.visibility = View.VISIBLE

                binding.tvEdit.text = getString(R.string.save)

                binding.etTitle.setText(notesName)
                binding.etDescription.setText(notes)
            }
        }

        binding.tvEdit.setOnClickListener {
            if (binding.tvEdit.text == getString(R.string.save)) {
                viewModel.updateNote(noteId, NewNote(binding.etTitle.text.toString(), binding.etDescription.text.toString()))
            } else {
                binding.tvTitle.visibility = View.GONE
                binding.tvNotesDetails.visibility = View.GONE
                binding.etTitle.visibility = View.VISIBLE
                binding.etDescription.visibility = View.VISIBLE

                binding.tvEdit.text = getString(R.string.save)

                binding.etTitle.setText(notesName)
                binding.etDescription.setText(notes)
            }
        }
    }

    private fun markdownNotes() {
        binding.tvTitle.text = notesName

        val markwon = Markwon.create(requireContext())
        markwon.setMarkdown(binding.tvNotesDetails, notes)
        Linkify.addLinks(binding.tvNotesDetails, Linkify.WEB_URLS)
    }
}