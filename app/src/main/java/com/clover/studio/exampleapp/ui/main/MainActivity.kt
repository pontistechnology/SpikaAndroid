package com.clover.studio.exampleapp.ui.main

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.asLiveData
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.Message
import com.clover.studio.exampleapp.databinding.ActivityMainBinding
import com.clover.studio.exampleapp.ui.main.chat.startChatScreenActivity
import com.clover.studio.exampleapp.ui.onboarding.startOnboardingActivity
import com.clover.studio.exampleapp.utils.Const
import com.clover.studio.exampleapp.utils.EventObserver
import com.clover.studio.exampleapp.utils.SSEListener
import com.clover.studio.exampleapp.utils.Tools
import com.clover.studio.exampleapp.utils.dialog.DialogError
import com.clover.studio.exampleapp.utils.extendables.BaseActivity
import com.clover.studio.exampleapp.utils.extendables.DialogInteraction
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import timber.log.Timber


fun startMainActivity(fromActivity: Activity) = fromActivity.apply {
    startActivity(Intent(fromActivity as Context, MainActivity::class.java))
    finish()
}

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private var mutedRooms: MutableList<String> = ArrayList()

    private val viewModel: MainViewModel by viewModels()
    private lateinit var bindingSetup: ActivityMainBinding
    private var handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = Runnable {
        Timber.d("Ending handler")
        bindingSetup.cvNotification.cvRoot.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingSetup = ActivityMainBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        initializeObservers()
        sendPushTokenToServer()

//        deleteRooms()
    }

    /**
     * Mock function for deleting rooms if you have any unwanted rooms in the local database.
     */
    private fun deleteRooms() {
        val roomIds = listOf<Int>(540)

        for (id in roomIds) {
            viewModel.deleteRoom(id)
        }
    }

    private fun initializeObservers() {
        viewModel.tokenExpiredListener.observe(this, EventObserver { tokenExpired ->
            if (tokenExpired) {
                DialogError.getInstance(this,
                    getString(R.string.warning),
                    getString(R.string.session_expired),
                    null,
                    getString(R.string.ok),
                    object : DialogInteraction {
                        override fun onFirstOptionClicked() {
                            // ignore
                        }

                        override fun onSecondOptionClicked() {
                            viewModel.setTokenExpiredFalse()
                            startOnboardingActivity(this@MainActivity, false)
                        }
                    })
            }
        })

        viewModel.userSettingsListener.observe(this, EventObserver {
            when (it) {
                is UserSettingsFetched -> {
                    for (setting in it.settings) {
                        mutedRooms.add(setting.key)
                    }
                }

                UserSettingsFetchFailed -> Timber.d("Failed to fetch user settings")
                else -> Timber.d("Other error")
            }
        })

        viewModel.roomDataListener.observe(this, EventObserver {
            when (it) {
                is SingleRoomData -> {
                    val gson = Gson()
                    val roomData = gson.toJson(it.roomData.roomWithUsers)
                    startChatScreenActivity(this, roomData)
                }
                SingleRoomFetchFailed -> Timber.d("Failed to fetch room data")
                else -> Timber.d("Other error")
            }
        })

        viewModel.roomNotificationListener.observe(this, EventObserver {
            when (it) {
                is RoomNotificationData -> {
                    val myUserId = viewModel.getLocalUserId()
                    var isRoomMuted = false
                    for (key in mutedRooms) {
                        if (key.contains(it.message.roomId.toString())) {
                            isRoomMuted = true
                            break
                        }
                    }

                    if (myUserId == it.message.fromUserId || isRoomMuted) return@EventObserver
                    runOnUiThread {
                        val animator =
                            ValueAnimator.ofInt(bindingSetup.cvNotification.pbTimeout.max, 0)
                        animator.duration = 5000
                        animator.addUpdateListener { animation ->
                            bindingSetup.cvNotification.pbTimeout.progress =
                                animation.animatedValue as Int
                        }
                        animator.start()

                        if (it.roomWithUsers.room.type.equals(Const.JsonFields.GROUP)) {
                            Glide.with(this@MainActivity)
                                .load(it.roomWithUsers.room.avatarUrl?.let { avatarUrl ->
                                    Tools.getFileUrl(
                                        avatarUrl
                                    )
                                })
                                .into(bindingSetup.cvNotification.ivUserImage)
                            bindingSetup.cvNotification.tvTitle.text = it.roomWithUsers.room.name
                            for (user in it.roomWithUsers.users) {
                                if (user.id != myUserId && user.id == it.message.fromUserId) {
                                    val content = if (it.message.type != Const.JsonFields.TEXT) {
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
                            for (user in it.roomWithUsers.users) {
                                if (user.id != myUserId && user.id == it.message.fromUserId) {
                                    Glide.with(this@MainActivity)
                                        .load(user.avatarUrl?.let { avatarUrl ->
                                            Tools.getFileUrl(
                                                avatarUrl
                                            )
                                        })
                                        .into(bindingSetup.cvNotification.ivUserImage)
                                    val content = if (it.message.type != Const.JsonFields.TEXT) {
                                        getString(
                                            R.string.generic_shared,
                                            it.message.type.toString()
                                                .replaceFirstChar { type -> type.uppercase() })
                                    } else {
                                        it.message.body?.text.toString()
                                    }

                                    bindingSetup.cvNotification.tvTitle.text = user.displayName
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

                        Timber.d("Starting handler")
                        runnable.let { runnable -> handler.removeCallbacks(runnable) }

                        handler = Handler(Looper.getMainLooper())
                        Timber.d("Starting handler")
                        handler.postDelayed(runnable, 5000)
                    }
                }
                is RoomWithUsersFailed -> Timber.d("Failed to fetch room with users")
                else -> Timber.d("Other error")
            }
        })
    }

    private fun sendPushTokenToServer() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            val jsonObject = JsonObject()

            jsonObject.addProperty(Const.JsonFields.PUSH_TOKEN, token)

            viewModel.updatePushToken(jsonObject)
            Timber.d("Token: $token")
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.getUserSettings()
        viewModel.getPushNotificationStream(object : SSEListener {
            override fun newMessageReceived(message: Message) {
                Timber.d("Message received")
                message.roomId?.let { viewModel.getRoomWithUsers(it, message) }
            }
        }).asLiveData(Dispatchers.IO).observe(this) {
            Timber.d("Message $it")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val extras = intent?.extras
        if (extras != null) {
            Timber.d("Extras: ${extras.get(Const.IntentExtras.ROOM_ID_EXTRA)}")
            try {
                viewModel.getSingleRoomData(extras.get(Const.IntentExtras.ROOM_ID_EXTRA) as Int)
            } catch (ex: Exception) {
                // ignore
            }
            intent.removeExtra(Const.IntentExtras.ROOM_ID_EXTRA)
        }
    }
}