package com.clover.studio.exampleapp.ui.main.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.databinding.ActivityChatScreenBinding

class ChatScreenActivity : AppCompatActivity() {

    private lateinit var bindingSetup: ActivityChatScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityChatScreenBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)
    }
}