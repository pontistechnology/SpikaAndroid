package com.clover.studio.spikamessenger.ui.main.chat

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.clover.studio.spikamessenger.databinding.AudioLayoutBinding


class AudioLayout(context: Context) :
    ConstraintLayout(context) {

    private var bindingSetup: AudioLayoutBinding = AudioLayoutBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    private val binding get() = bindingSetup

    private fun bindAudio() = with(binding) {}
}
