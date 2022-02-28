package com.clover.studio.exampleapp.ui.main.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.data.models.User
import com.clover.studio.exampleapp.databinding.ActivityChatScreenBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

fun startChatScreenActivity(fromActivity: Activity, userData: String) = fromActivity.apply {
    val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
    intent.putExtra(Const.Navigation.USER_PROFILE, userData)
    startActivity(intent)
}

@AndroidEntryPoint
class ChatScreenActivity : AppCompatActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var user: User
    private lateinit var bindingSetup: ActivityChatScreenBinding
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityChatScreenBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        // Fetch user data sent from previous activity
        val gson = Gson()
        user = gson.fromJson(
            intent.getStringExtra(Const.Navigation.USER_PROFILE),
            User::class.java
        )

        initViews()
        setUpAdapter()
        initializeObservers()
    }

    private fun initializeObservers() {
        viewModel.messageSendListener.observe(this, EventObserver {
            when (it) {
                ChatStates.MESSAGE_SENT -> Timber.d("Message sent successfully")
                ChatStates.MESSAGE_SEND_FAIL -> Timber.d("Message send fail")
                else -> Timber.d("Other error")
            }
        })
    }

    private fun setUpAdapter() {
        chatAdapter = ChatAdapter(this) {
            // TODO set up on click function
        }

        bindingSetup.rvChat.adapter = chatAdapter
        bindingSetup.rvChat.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        // TODO send fetched list of messages to adapter
    }

    private fun initViews() {
        bindingSetup.ivArrowBack.setOnClickListener {
            finish()
        }

        // TODO add send message button and handle UI when message is being entered
        // Change required field after work has been done
        bindingSetup.tvUsername.text = user.displayName
        Glide.with(this).load(user.avatarUrl).into(bindingSetup.ivUserImage)
        bindingSetup.ivMicrophone.setOnClickListener {
            val jsonObject = JsonObject()
            val innerObject = JsonObject()
            innerObject.addProperty(Const.JsonFields.TEXT, bindingSetup.etMessage.text.toString())
            innerObject.addProperty(Const.JsonFields.TYPE, "text")

            jsonObject.addProperty(Const.JsonFields.ROOM_ID, 1)
            jsonObject.addProperty(Const.JsonFields.TYPE, "text")
            jsonObject.add(Const.JsonFields.MESSAGE, innerObject)

            viewModel.sendMessage(jsonObject)
        }
    }

    override fun onBackPressed() {
        finish()
    }
}