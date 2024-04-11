package com.clover.studio.spikamessenger.ui.main.chat.media

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.clover.studio.spikamessenger.R
import com.clover.studio.spikamessenger.databinding.FragmentMediaPreparationBinding

class MediaPreparationFragment : Fragment() {

    private var bindingSetup: FragmentMediaPreparationBinding? = null
    private val binding get() = bindingSetup!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingSetup = FragmentMediaPreparationBinding.inflate(inflater, container, false)

        return binding.root
    }
}
