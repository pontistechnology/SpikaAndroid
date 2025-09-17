package com.clover.studio.spikamessenger.ui.main.chat.bottom_sheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clover.studio.spikamessenger.databinding.BottomSheetMediaBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MediaBottomSheet(
    private val context: Context
) :
    BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetMediaBinding
    private var listener: BottomSheetMediaAction? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetMediaBinding.inflate(layoutInflater)

        initializeViews()


        return binding.root
    }

    companion object {
        const val TAG = "mediaSheet"
    }

    interface BottomSheetMediaAction {
        fun chooseFileAction()
    }

    fun setActionListener(listener: BottomSheetMediaAction) {
        this.listener = listener
    }

    private fun initializeViews() {
        binding.ivRemove.setOnClickListener {
            dismiss()
        }

        binding.btnFiles.setOnClickListener {
            listener?.chooseFileAction()
            dismiss()
        }

        binding.btnLibrary.setOnClickListener {
            listener?.chooseFileAction()
            dismiss()
        }

//        TODO this will be implemented later
//        binding.btnContact.setOnClickListener {
//            dismiss()
//        }
//
//        binding.btnLocation.setOnClickListener {
//            dismiss()
//        }
    }
}
