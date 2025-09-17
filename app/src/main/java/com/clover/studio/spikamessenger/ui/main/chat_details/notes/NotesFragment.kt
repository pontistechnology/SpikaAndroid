package com.clover.studio.spikamessenger.ui.main.chat_details.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.FragmentNotesBinding
import com.clover.studio.spikamessenger.ui.main.MainActivity
import com.clover.studio.spikamessenger.ui.main.chat.ChatViewModel
import com.clover.studio.spikamessenger.utils.Const
import com.clover.studio.spikamessenger.utils.NotesSwipeHelper
import com.clover.studio.spikamessenger.utils.Tools
import com.clover.studio.spikamessenger.utils.dialog.DialogError
import com.clover.studio.spikamessenger.utils.extendables.BaseFragment
import com.clover.studio.spikamessenger.utils.extendables.DialogInteraction
import com.clover.studio.spikamessenger.utils.helpers.Resource

class NotesFragment : BaseFragment() {
    private var bindingSetup: FragmentNotesBinding? = null
    private val viewModel: ChatViewModel by activityViewModels()
    private val binding get() = bindingSetup!!
    private lateinit var adapter: NotesAdapter
    private val args: NotesFragmentArgs by navArgs()
    private var roomId: Int = 0

    private var itemTouchHelper: ItemTouchHelper? = null
    private var navOptionsBuilder: NavOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = args.roomId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentNotesBinding.inflate(inflater, container, false)

        navOptionsBuilder = Tools.createCustomNavOptions()

        setupAdapter()
        initializeViews()
        initializeObservers()
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun initializeViews() {
        binding.ivBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.ivNewNote.setOnClickListener {
            if (activity is MainActivity) {
                findNavController().navigate(
                    R.id.newNoteFragment,
                    bundleOf(Const.Navigation.ROOM_ID to roomId),
                    navOptionsBuilder
                )
            } else findNavController().navigate(
                R.id.newNoteFragment,
                bundleOf(Const.Navigation.ROOM_ID to roomId),
                navOptionsBuilder
            )
        }

        if (itemTouchHelper == null) {
            val notesSwipeController =
                NotesSwipeHelper(
                    requireContext(),
                    onSwipeAction = { position ->
                        DialogError.getInstance(requireContext(),
                            getString(R.string.delete_note),
                            getString(R.string.delete_note_description),
                            getString(R.string.no),
                            getString(R.string.yes),
                            object : DialogInteraction {
                                override fun onSecondOptionClicked() {
                                    val note = adapter.currentList[position]
                                    viewModel.deleteNote(note.id)
                                }
                            })
                    })

            itemTouchHelper = ItemTouchHelper(notesSwipeController)
        }
        itemTouchHelper?.attachToRecyclerView(bindingSetup?.rvNotes)
    }

    private fun initializeObservers() {
        viewModel.getRoomNotes(roomId).observe(viewLifecycleOwner) {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    if (it.responseData?.isNotEmpty() == true) {
                        adapter.submitList(it.responseData)
                        binding.tvNoNotes.visibility = View.GONE
                    } else binding.tvNoNotes.visibility = View.VISIBLE
                }

                Resource.Status.LOADING -> {
                    // Loading
                }

                else -> {
                    // Error
                }
            }
        }

        viewModel.fetchNotes(roomId)
    }

    private fun setupAdapter() {
        adapter = NotesAdapter(requireActivity()) {
            if (activity is MainActivity) {
                findNavController().navigate(
                    R.id.notesDetailsFragment,
                    bundleOf(
                        Const.Navigation.NOTE_ID to it.id,
                        Const.Navigation.NOTES_DETAILS to it.content,
                        Const.Navigation.NOTES_NAME to it.title
                    ),
                    navOptionsBuilder
                )
            } else findNavController().navigate(
                R.id.notesDetailsFragment,
                bundleOf(
                    Const.Navigation.NOTE_ID to it.id,
                    Const.Navigation.NOTES_DETAILS to it.content,
                    Const.Navigation.NOTES_NAME to it.title
                ),
                navOptionsBuilder
            )
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
