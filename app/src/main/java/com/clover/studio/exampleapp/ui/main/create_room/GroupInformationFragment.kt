package com.clover.studio.exampleapp.ui.main.create_room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.UserAndPhoneUser
import com.clover.studio.exampleapp.databinding.FragmentGroupInformationBinding
import com.clover.studio.exampleapp.ui.main.MainViewModel
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.dialog.DialogInteraction
import timber.log.Timber

class GroupInformationFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: GroupInformationAdapter
    private var selectedUsers: MutableList<UserAndPhoneUser> = ArrayList()

    private var bindingSetup: FragmentGroupInformationBinding? = null

    private val binding get() = bindingSetup!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (requireArguments().getParcelableArrayList<UserAndPhoneUser>(Const.Navigation.SELECTED_USERS) == null) {
            DialogError.getInstance(requireActivity(),
                getString(R.string.error),
                getString(R.string.failed_user_data),
                null,
                getString(R.string.ok),
                object : DialogInteraction {
                    override fun onFirstOptionClicked() {
                        // ignore
                    }

                    override fun onSecondOptionClicked() {
                        // ignore
                    }
                })
            Timber.d("Failed to fetch user data")
        } else {
            selectedUsers =
                requireArguments().getParcelableArrayList(Const.Navigation.SELECTED_USERS)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentGroupInformationBinding.inflate(inflater, container, false)

        setupAdapter()
        initializeObservers()
        initializeViews()

        return binding.root
    }

    private fun initializeViews() {
        binding.tvPeopleSelected.text = getString(R.string.s_people_selected, selectedUsers.size)
        adapter.submitList(selectedUsers)

        binding.etEnterUsername.addTextChangedListener {
            if (binding.etEnterUsername.text.isNotEmpty()) {
                binding.tvCreate.isClickable = true
                binding.tvCreate.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary_color
                    )
                )
            } else {
                binding.tvCreate.isClickable = false
                binding.tvCreate.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_tertiary
                    )
                )
            }
        }
    }

    private fun initializeObservers() {
        // TODO("Not yet implemented")
    }

    private fun setupAdapter() {
        adapter = GroupInformationAdapter(requireContext()) {
            selectedUsers.remove(it)
            adapter.submitList(selectedUsers)
            binding.tvPeopleSelected.text =
                getString(R.string.s_people_selected, selectedUsers.size)
            adapter.notifyDataSetChanged()
        }

        binding.rvContacts.adapter = adapter
        binding.rvContacts.layoutManager =
            LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
    }
}