package com.clover.studio.exampleapp.ui.main.chat

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.Message
import com.clover.studio.exampleapp.data.models.junction.RoomWithUsers
import com.clover.studio.exampleapp.databinding.ActivityChatScreenBinding
import com.clover.studio.exampleapp.ui.onboarding.startOnboardingActivity
import com.clover.studio.exampleapp.utils.*
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseActivity
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.clover.studio.exampleapp.utils.helpers.GsonProvider
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


fun startChatScreenActivity(fromActivity: Activity, roomData: String) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomData)
        startActivity(intent)
    }

fun replaceChatScreenActivity(fromActivity: Activity, roomData: String) =
    fromActivity.apply {
        val intent = Intent(fromActivity as Context, ChatScreenActivity::class.java)
        intent.putExtra(Const.Navigation.ROOM_DATA, roomData)
        startActivity(intent)
        finish()
    }

@AndroidEntryPoint
class ChatScreenActivity : BaseActivity(), SSEListener {
    var roomWithUsers: RoomWithUsers? = null

    private lateinit var bindingSetup: ActivityChatScreenBinding
    private val viewModel: ChatViewModel by viewModels()
    private var handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = Runnable {
        Timber.d("Ending handler")
        bindingSetup.cvNotification.cvRoot.visibility = View.GONE
    }

    @Inject
    lateinit var uploadDownloadManager: UploadDownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityChatScreenBinding.inflate(layoutInflater)

        val view = bindingSetup.root
        setContentView(view)

        // Fetch room data sent from previous activity
        val gson = GsonProvider.gson
        roomWithUsers = gson.fromJson(
            intent.getStringExtra(Const.Navigation.ROOM_DATA),
            RoomWithUsers::class.java
        )

        Timber.d("chatScreen ${roomWithUsers.toString()}")
        initializeObservers()
    }

    private fun initializeObservers() {
        viewModel.setupSSEManager(this)

        viewModel.roomDataListener.observe(this, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    val gson = GsonProvider.gson
                    val roomData = gson.toJson(it.responseData?.roomWithUsers)
                    replaceChatScreenActivity(this, roomData)
                }
                Resource.Status.ERROR -> Timber.d("Failed to fetch room data")
                else -> Timber.d("Other error")
            }
        })

        viewModel.roomNotificationListener.observe(this, EventObserver {
            when (it.response.status) {
                Resource.Status.SUCCESS -> {
                    val myUserId = viewModel.getLocalUserId()

                    if (myUserId == it.message.fromUserId || roomWithUsers?.room?.roomId == it.message.roomId || it.response.responseData?.room!!.muted) return@EventObserver
                    runOnUiThread {
                        val animator =
                            ValueAnimator.ofInt(
                                bindingSetup.cvNotification.pbTimeout.max,
                                0
                            )
                        animator.duration = 5000
                        animator.addUpdateListener { animation ->
                            bindingSetup.cvNotification.pbTimeout.progress =
                                animation.animatedValue as Int
                        }
                        animator.start()

                        if (it.response.responseData.room.type.equals(Const.JsonFields.GROUP)) {
                            Glide.with(this@ChatScreenActivity)
                                .load(it.response.responseData.room.avatarFileId?.let { fileId ->
                                    Tools.getFilePathUrl(
                                        fileId
                                    )
                                })
                                .placeholder(R.drawable.img_user_placeholder)
                                .centerCrop()
                                .into(bindingSetup.cvNotification.ivUserImage)
                            bindingSetup.cvNotification.tvTitle.text =
                                it.response.responseData.room.name
                            for (user in it.response.responseData.users) {
                                if (user.id != myUserId && user.id == it.message.fromUserId) {
                                    val content: String =
                                        if (it.message.type != Const.JsonFields.TEXT_TYPE) {
                                            user.displayName + ": " + getString(
                                                R.string.generic_shared,
                                                it.message.type.toString()
                                                    .replaceFirstChar { type -> type.uppercase() })
                                        } else {
                                            user.displayName + ": " + it.message.body?.text.toString()
                                        }

                                    bindingSetup.cvNotification.tvMessage.text =
                                        content
                                    break
                                }
                            }
                        } else {
                            for (user in it.response.responseData.users) {
                                if (user.id != myUserId && user.id == it.message.fromUserId) {
                                    Glide.with(this@ChatScreenActivity)
                                        .load(user.avatarFileId?.let { fileId ->
                                            Tools.getFilePathUrl(
                                                fileId
                                            )
                                        })
                                        .placeholder(R.drawable.img_user_placeholder)
                                        .centerCrop()
                                        .into(bindingSetup.cvNotification.ivUserImage)
                                    val content: String =
                                        if (it.message.type != Const.JsonFields.TEXT_TYPE) {
                                            getString(
                                                R.string.generic_shared,
                                                it.message.type.toString()
                                                    .replaceFirstChar { type -> type.uppercase() })
                                        } else {
                                            it.message.body?.text.toString()
                                        }

                                    bindingSetup.cvNotification.tvTitle.text =
                                        user.displayName
                                    bindingSetup.cvNotification.tvMessage.text =
                                        content
                                    break
                                }
                            }
                        }


                        bindingSetup.cvNotification.cvRoot.visibility = View.VISIBLE

                        val roomId = it.message.roomId
                        bindingSetup.cvNotification.cvRoot.setOnClickListener {
                            roomId?.let { roomId -> viewModel.getSingleRoomData(roomId) }
                        }

                        runnable.let { runnable -> handler.removeCallbacks(runnable) }

                        handler = Handler(Looper.getMainLooper())
                        Timber.d("Starting handler")
                        handler.postDelayed(runnable, 5000)
                    }
                }
                Resource.Status.ERROR -> Timber.d("Failed to fetch room with users")
                else -> Timber.d("Other error")
            }
        })

        viewModel.tokenExpiredListener.observe(this, EventObserver { tokenExpired ->
            if (tokenExpired) {
                DialogError.getInstance(this,
                    getString(R.string.warning),
                    getString(R.string.session_expired),
                    null,
                    getString(R.string.ok),
                    object : DialogInteraction {
                        override fun onFirstOptionClicked() {
                            // Ignore
                        }

                        override fun onSecondOptionClicked() {
                            viewModel.setTokenExpiredFalse()
                            startOnboardingActivity(this@ChatScreenActivity, false)
                        }
                    })
            }
        })
    }

    override fun onBackPressed() {
        val fragment =
            this.supportFragmentManager.findFragmentById(R.id.main_chat_container) as? NavHostFragment
        val currentFragment =
            fragment?.childFragmentManager?.fragments?.get(0) as? ChatOnBackPressed

        // Check why this returns null if upload is not in progress
        currentFragment?.onBackPressed()?.takeIf { !it }.let {
            Timber.d("Boolean: $it")
            if (it == null) {
                super.onBackPressed()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.getUnreadCount()
    }

    override fun newMessageReceived(message: Message) {
        Timber.d("Message received")
        message.roomId?.let { viewModel.getRoomWithUsers(it, message) }
    }
}

interface ChatOnBackPressed {
    fun onBackPressed(): Boolean
}