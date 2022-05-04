package com.clover.studio.exampleapp.ui.main.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.data.models.ChatRoom
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.data.models.MessageBody
import com.clover.studio.exampleapp.databinding.ActivityChatScreenBinding
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.Tools.getAvatarUrl
import com.clover.studio.exampleapp.utils.extendables.BaseActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


fun startChatScreenActivity(fromActivity: Activity, roomData: String) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomData)
        startActivity(intent)
    }

private const val ROTATION_ON = 45f
private const val ROTATION_OFF = 0f

@AndroidEntryPoint
class ChatScreenActivity : BaseActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var room: ChatRoom
    private lateinit var bindingSetup: ActivityChatScreenBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messages: MutableList<Message>
    private var unsentMessages: MutableList<Message> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityChatScreenBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        val gson = Gson()
        // Fetch room data sent from previous activity
        room = gson.fromJson(
            intent.getStringExtra(Const.Navigation.ROOM_DATA),
            ChatRoom::class.java
        )

        initViews()
        setUpAdapter()
        initializeObservers()
    }

    private fun initializeObservers() {
        viewModel.messageSendListener.observe(this, EventObserver {
            when (it) {
                ChatStatesEnum.MESSAGE_SENT -> {
                    bindingSetup.etMessage.setText("")
                    viewModel.getMessages(room.id)
                }
                ChatStatesEnum.MESSAGE_SEND_FAIL -> Timber.d("Message send fail")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getMessagesListener.observe(this, EventObserver {
            when (it) {
                is MessagesFetched -> {
                    viewModel.deleteLocalMessages(unsentMessages)
                    unsentMessages.clear()
                }
                is MessageFetchFail -> Timber.d("Failed to fetch messages")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getMessagesTimestampListener.observe(this, EventObserver {
            when (it) {
                is MessagesTimestampFetched -> {
                    Timber.d("Messages timestamp fetched")
                    messages = it.messages as MutableList<Message>
                    chatAdapter.submitList(it.messages)
                }
                is MessageTimestampFetchFail -> Timber.d("Failed to fetch messages timestamp")
                else -> Timber.d("Other error")
            }
        })

        viewModel.sendMessageDeliveredListener.observe(this, EventObserver {
            when (it) {
                ChatStatesEnum.MESSAGE_DELIVERED -> Timber.d("Messages delivered")
                ChatStatesEnum.MESSAGE_DELIVER_FAIL -> Timber.d("Failed to deliver messages")
                else -> Timber.d("Other error")
            }
        })

        viewModel.getLocalMessages(room.id).observe(this) {
            if (it.isNotEmpty()) {
                messages = it as MutableList<Message>
                messages.sortByDescending { message -> message.createdAt }
                chatAdapter.submitList(messages) {
                    bindingSetup.rvChat.scrollToPosition(0)
                }
            }
        }
    }

    private fun setUpAdapter() {
        chatAdapter = ChatAdapter(this, viewModel.getLocalUserId()!!) {
            // TODO set up on click function
        }

        bindingSetup.rvChat.adapter = chatAdapter
        bindingSetup.rvChat.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, true)

        // Add callback for item swipe handling
        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.RIGHT
            ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // ignore
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                // Get swiped message text and add to message EditText
                // After that, return item to correct position
                val position = viewHolder.adapterPosition
                bindingSetup.etMessage.setText(messages[position].body?.text)
                chatAdapter.notifyItemChanged(position)
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(bindingSetup.rvChat)

        // Get user messages
        viewModel.getMessages(room.id)
    }

    private fun initViews() {
        bindingSetup.ivArrowBack.setOnClickListener {
            finish()
        }

        bindingSetup.etMessage.addTextChangedListener {
            if (it?.isNotEmpty() == true) {
                bindingSetup.ivCamera.visibility = View.GONE
                bindingSetup.ivMicrophone.visibility = View.GONE
                bindingSetup.ivButtonSend.visibility = View.VISIBLE
                bindingSetup.clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    endToStart = bindingSetup.ivButtonSend.id
                }
                bindingSetup.ivAdd.rotation = ROTATION_ON
            } else {
                bindingSetup.ivCamera.visibility = View.VISIBLE
                bindingSetup.ivMicrophone.visibility = View.VISIBLE
                bindingSetup.ivButtonSend.visibility = View.GONE
                bindingSetup.clTyping.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    endToStart = bindingSetup.ivCamera.id
                }
                bindingSetup.ivAdd.rotation = ROTATION_OFF
            }
        }

        // TODO add send message button and handle UI when message is being entered
        // Change required field after work has been done

        if (Const.JsonFields.PRIVATE == room.type) {
            room.users?.forEach { roomUser ->
                if (viewModel.getLocalUserId().toString() != roomUser.userId.toString()) {
                    bindingSetup.tvChatName.text = roomUser.user?.displayName
                    Glide.with(this)
                        .load(roomUser.user?.avatarUrl?.let { getAvatarUrl(it) })
                        .into(bindingSetup.ivUserImage)
                }
            }
        } else {
            bindingSetup.tvChatName.text = room.name
            Glide.with(this).load(room.avatarUrl?.let { getAvatarUrl(it) })
                .into(bindingSetup.ivUserImage)
        }

        bindingSetup.ivButtonSend.setOnClickListener {
            // TODO success and fail states.
            val tempMessage = Message(
                0,
                viewModel.getLocalUserId(),
                0,
                0,
                0,
                -1,
                0,
                room.id,
                "text",
                MessageBody(bindingSetup.etMessage.text.toString(), "text"),
                System.currentTimeMillis()
            )

            unsentMessages.add(tempMessage)
            viewModel.storeMessageLocally(tempMessage)

            val jsonObject = JsonObject()
            val innerObject = JsonObject()
            innerObject.addProperty(
                Const.JsonFields.TEXT,
                bindingSetup.etMessage.text.toString()
            )
            innerObject.addProperty(Const.JsonFields.TYPE, "text")

            jsonObject.addProperty(Const.JsonFields.ROOM_ID, room.id)
            jsonObject.addProperty(Const.JsonFields.TYPE, "text")
            jsonObject.add(Const.JsonFields.BODY, innerObject)

            viewModel.sendMessage(jsonObject)
        }
    }

    override fun onBackPressed() {
        finish()
    }
}