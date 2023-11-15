package com.clover.studio.spikamessenger.ui.main.chat_details.notes

import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.data.models.networking.NewNote
import com.clover.studio.spikamessenger.databinding.FragmentNotesDetailsBinding
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.utils.EventObserver
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.helpers.Resource
import io.noties.markwon.Markwon
import timber.log.Timber

class NotesDetailsFragment : BaseFragment() {
    private var bindingSetup: FragmentNotesDetailsBinding? = null
    private val binding get() = bindingSetup!!
    private val args: NotesDetailsFragmentArgs by navArgs()
    private val viewModel: ChatViewModel by activityViewModels()
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
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    binding.tvTitle.visibility = View.VISIBLE
                    binding.tvNotesDetails.visibility = View.VISIBLE
                    binding.etTitle.visibility = View.GONE
                    binding.etDescription.visibility = View.GONE

                    notesName = binding.etTitle.text.toString()
                    notes = binding.etDescription.text.toString()

                    markdownNotes()
                }

                Resource.Status.ERROR -> Toast.makeText(
                    context,
                    getString(R.string.note_creation_failed),
                    Toast.LENGTH_SHORT
                ).show()

                else -> {
                    // Deleted
                    activity?.onBackPressedDispatcher?.onBackPressed()
                    Timber.d("Other error")
                }
            }
        })

        viewModel.noteDeletionListener.observe(viewLifecycleOwner, EventObserver {
            when (it.response.status) {
                Resource.Status.SUCCESS -> {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }

                Resource.Status.ERROR -> {
                    Toast.makeText(
                        context,
                        getString(R.string.failed_delete_note),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> Timber.d("Other error")
            }
        })
    }

    private fun initializeViews() = with(binding) {
        markdownNotes()

        ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        ivSave.setOnClickListener {
            viewModel.updateNote(
                noteId,
                NewNote(etTitle.text.toString(), etDescription.text.toString())
            )

            tvTitle.visibility = View.VISIBLE
            tvNotesDetails.visibility = View.VISIBLE
            etTitle.visibility = View.GONE
            etDescription.visibility = View.GONE

            ivSave.visibility = View.GONE
            ivEdit.visibility = View.VISIBLE

        }

        ivEdit.setOnClickListener {
            tvTitle.visibility = View.GONE
            tvNotesDetails.visibility = View.GONE
            etTitle.visibility = View.VISIBLE
            etDescription.visibility = View.VISIBLE


            etTitle.setText(notesName)
            etDescription.setText(notes)

            ivSave.visibility = View.VISIBLE
            ivEdit.visibility = View.GONE
        }

    }

    private fun markdownNotes() = with(binding) {
        tvTitle.text = notesName

        val markwon = Markwon.create(requireContext())
        markwon.setMarkdown(tvNotesDetails, notes)
        Linkify.addLinks(tvNotesDetails, Linkify.WEB_URLS)

        ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }
}
