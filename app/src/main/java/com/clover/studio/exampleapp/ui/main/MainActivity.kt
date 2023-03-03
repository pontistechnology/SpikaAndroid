package com.clover.studio.exampleapp.ui.main

import android.animation.ValueAnimator
import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
import androidx.lifecycle.asLiveData
import com.bumptech.glide.Glide
import com.clover.studio.exampleapp.R
import com.clover.studio.exampleapp.data.models.entity.Message
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
import com.clover.studio.exampleapp.utils.helpers.Resource
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


fun startMainActivity(fromActivity: Activity) = fromActivity.apply {
    startActivity(Intent(fromActivity as Context, MainActivity::class.java))
    finish()
}

@AndroidEntryPoint
class MainActivity : BaseActivity(), SSEListener {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var bindingSetup: ActivityMainBinding
    private var handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = Runnable {
        Timber.d("Ending handler")
        bindingSetup.cvNotification.cvRoot.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.getUserTheme() == MODE_NIGHT_UNSPECIFIED) {
            val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            when (uiModeManager.nightMode) {
                UiModeManager.MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        } else {
            AppCompatDelegate.setDefaultNightMode(viewModel.getUserTheme()!!)
        }

        bindingSetup = ActivityMainBinding.inflate(layoutInflater)
        val view = bindingSetup.root
        setContentView(view)

        initializeObservers()
        sendPushTokenToServer()
        checkIntentExtras()
    }

    private fun checkIntentExtras() {
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

    private fun initializeObservers() {
        viewModel.setupSSEManager(this)

        viewModel.roomDataListener.observe(this, EventObserver {
            when (it.status) {
                Resource.Status.SUCCESS -> {
                    val gson = Gson()
                    val roomData = gson.toJson(it.responseData?.roomWithUsers)
                    startChatScreenActivity(this, roomData)
                    Timber.d("Main Success!")
                }
                Resource.Status.ERROR -> Timber.d("Failed to fetch room data")
                else -> Timber.d("Other error")
            }
        })

        viewModel.roomNotificationListener.observe(this, EventObserver {
            when (it.response.status) {
                Resource.Status.SUCCESS -> {
                    val myUserId = viewModel.getLocalUserId()

                    if (myUserId == it.message.fromUserId || it.response.responseData?.room?.muted == true) return@EventObserver
                    runOnUiThread {
                        val animator =
                            ValueAnimator.ofInt(bindingSetup.cvNotification.pbTimeout.max, 0)
                        animator.duration = 5000
                        animator.addUpdateListener { animation ->
                            bindingSetup.cvNotification.pbTimeout.progress =
                                animation.animatedValue as Int
                        }
                        animator.start()

                        if (it.response.responseData!!.room.type.equals(Const.JsonFields.GROUP)) {
                            Timber.d("Showing room image")
                            Glide.with(this@MainActivity)
                                .load(it.response.responseData.room.avatarFileId?.let { fileId ->
                                    Tools.getFilePathUrl(
                                        fileId
                                    )
                                })
                                .placeholder(R.drawable.img_user_placeholder)
                                .centerCrop()
                                .into(bindingSetup.cvNotification.ivUserImage)
                            bindingSetup.cvNotification.tvTitle.text = it.response.responseData.room.name
                            for (user in it.response.responseData.users) {
                                if (user.id != myUserId && user.id == it.message.fromUserId) {
                                    val content =
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
                                    Glide.with(this@MainActivity)
                                        .load(user.avatarFileId?.let { fileId ->
                                            Tools.getFilePathUrl(
                                                fileId
                                            )
                                        })
                                        .centerCrop()
                                        .placeholder(R.drawable.img_user_placeholder)
                                        .into(bindingSetup.cvNotification.ivUserImage)
                                    val content =
                                        if (it.message.type != Const.JsonFields.TEXT_TYPE) {
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
                            roomId?.let { roomId ->
                                run {
                                    viewModel.getSingleRoomData(roomId)
                                    bindingSetup.cvNotification.cvRoot.visibility = View.GONE
                                }
                            }
                        }

                        // Remove old instance of runnable if any is active. Prevents older
                        // notifications from removing newer ones.
                        Timber.d("Starting handler 1")
                        handler.removeCallbacks(runnable)

                        handler = Handler(Looper.getMainLooper())
                        Timber.d("Starting handler 2")
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
                            startOnboardingActivity(this@MainActivity, false)
                        }
                    })
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkIntentExtras()
    }

    override fun onStart() {
        super.onStart()
        Timber.d("First SSE launch = ${viewModel.checkIfFirstSSELaunch()}")
        if (viewModel.checkIfFirstSSELaunch()) {
            viewModel.getPushNotificationStream().asLiveData().observe(this) {}
        }

        viewModel.getUnreadCount()
    }

    override fun newMessageReceived(message: Message) {
        Timber.d("Message received")
        message.roomId?.let { viewModel.getRoomWithUsers(it, message) }
    }
}